package com.message.dto.websocket.outbound;

import com.message.constant.MessageType;
import com.message.constant.UserConnectionStatus;

public class DisconnectResponse extends BaseMessage {

	private final String username;
	private final UserConnectionStatus status;

	public DisconnectResponse(String username, UserConnectionStatus status) {
		super(MessageType.DISCONNECT_RESPONSE);
		this.username = username;
		this.status = status;
	}

	public String getUsername() {
		return username;
	}

	public UserConnectionStatus getStatus() {
		return status;
	}
}
