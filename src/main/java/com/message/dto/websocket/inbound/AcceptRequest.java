package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.message.constant.MessageType;

public class AcceptRequest extends BaseRequest {

	private final String username;

	@JsonCreator
	public AcceptRequest(
		@JsonProperty("username") String username
	) {
		super(MessageType.ACCEPT_REQUEST);
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
