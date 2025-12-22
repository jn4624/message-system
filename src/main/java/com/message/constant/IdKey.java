package com.message.constant;

public enum IdKey {

	HTTP_SESSION_ID("HTTP_SESSION_ID"),
	USER_ID("USER_ID"),
	/*
	   - CHANNEL_ID의 value를 소문자로 정의한 이유
	   - 해당 value는 Redis에 저장될 예정임
	   - Redis의 권장이 소문자 사용임
	 */
	CHANNEL_ID("channel_id");

	private final String value;

	IdKey(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
