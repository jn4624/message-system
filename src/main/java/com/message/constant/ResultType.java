package com.message.constant;

public enum ResultType {

	SUCCESS("Success"),
	FAILED("Failed"),
	INVALID_ARGS("Invalid arguments"),
	NOT_FOUND("Not found"),
	ALREADY_JOINED("Already joined"),
	OVER_LIMIT("Over limit"),
	NOT_JOINED("Not joined"),
	NOT_ALLOWED("Unconnected users included");

	private final String message;

	ResultType(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
