package com.message.auth;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageUserDetails implements UserDetails {

	private final Long userId;
	private final String username;
	private String password;

	@JsonCreator
	public MessageUserDetails(
		@JsonProperty("userId") Long userId,
		@JsonProperty("username") String username,
		@JsonProperty("password") String password
	) {
		this.userId = userId;
		this.username = username;
		this.password = password;
	}

	public Long getUserId() {
		return userId;
	}

	// 세션 정보 직렬화할 때 패스워드 정보를 지우기 위한 용도의 메서드
	public void erasePassword() {
		password = "";
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	@JsonIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// 권한 정보는 사용하지 않을거라 기본으로 유지
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageUserDetails that = (MessageUserDetails)o;
		return Objects.equals(getUsername(), that.getUsername());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getUsername());
	}
}
