package com.message.auth;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.message.dto.restapi.LoginRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestApiLoginAuthFilter extends AbstractAuthenticationProcessingFilter {

	/*
	  - JSON BODY로 들어올거라서 파싱할 ObjectMapper가 필요.
	  - Filter가 Bean으로 등록되지 않아 Spring Boot가 기본 제공해주는 ObjectMapper를 주입 받을 수 없다.
	    따라서 ObjectMapper 인스턴스를 직접 생성하여 사용한다.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	public RestApiLoginAuthFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthenticationManager authenticationManager) {
		super(requiresAuthenticationRequestMatcher, authenticationManager);
	}

	// 로그인 요청이 들어오면 호출되기 때문에 인증 과정을 구현해야 한다.
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
		// JSON 요청이 아닐 경우 파싱을 하지 못하니 튕겨낸다.
		if (!request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
			throw new AuthenticationServiceException("지원하지 않는 타입: " + request.getContentType());
		}

		LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
		// 토큰 생성
		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
			loginRequest.username(), loginRequest.password());
		// 인증 요청
		return getAuthenticationManager().authenticate(authenticationToken);
	}

	// 인증 성공시 호출된다.
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
		/*
		  - authResult를 context에 연결해줘야 한다.
		  - 연결해주면 현재 인증상태가 context에 세팅은 되었는데 저장되진 않는다.
		  - Security5까지는 자동 저장이 되었지만 Security6부터는 자동 저장이 되지 않아 개발자가 명시적으로 설정해줘야 한다.
		 */
		SecurityContext securityContext = SecurityContextHolder.getContext();
		// 세션 정보에 비밀번호 노출을 피하기 위해
		((MessageUserDetails) authResult.getPrincipal()).erasePassword();
		securityContext.setAuthentication(authResult);
		// contextRepository에 저장을 해야 인증 상태가 유지된다.
		HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
		contextRepository.saveContext(securityContext, request, response);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.TEXT_PLAIN_VALUE);
		response.getWriter().write(request.getSession().getId());
		response.getWriter().flush();
	}

	// 인증 실패시 호출된다.
	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.TEXT_PLAIN_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("인증 실패");
		response.getWriter().flush();
	}
}
