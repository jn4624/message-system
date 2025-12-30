package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.message.constant.MessageType;
import com.message.dto.domain.InviteCode;

public class JoinRequest extends BaseRequest {

	private final InviteCode inviteCode;

	@JsonCreator
	public JoinRequest(@JsonProperty("inviteCode") InviteCode inviteCode) {
		super(MessageType.JOIN_REQUEST);
		this.inviteCode = inviteCode;
	}

	public InviteCode getInviteCode() {
		return inviteCode;
	}
}
