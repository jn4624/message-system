package com.message.service;

import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

	private final SessionRepository<? extends Session> httpSessionRepository;

	public SessionService(SessionRepository<? extends Session> httpSessionRepository) {
		this.httpSessionRepository = httpSessionRepository;
	}

	public String getUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getName();
	}

	public void refreshTTL(String httpSessionId) {
		Session httpSession = httpSessionRepository.findById(httpSessionId);

		if (httpSession != null) {
			/*
			  - 세션의 마지막 접근 시간을 현재 시간으로 갱신하여 TTL 연장
			  - 아래 설정으로는 저장이 되지 않는다.
			    제네릭 제약사항은 읽기 전용이라 httpSessionRepository의 save를 호출할 수 없다.
			  - 따라서 RedisSessionConfig의 설정을 변경하는 방법으로 진행한다.
			 */
			httpSession.setLastAccessedTime(Instant.now());
		}
	}
}
