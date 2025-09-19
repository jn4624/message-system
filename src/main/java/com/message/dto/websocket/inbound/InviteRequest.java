package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.message.constant.MessageType;
import com.message.dto.domain.InviteCode;

public class InviteRequest extends BaseRequest {

	private final InviteCode userInviteCode;

	@JsonCreator
	public InviteRequest(
		@JsonProperty("userInviteCode") InviteCode userInviteCode
	) {
		super(MessageType.INVITE_REQUEST);
		this.userInviteCode = userInviteCode;
	}

	public InviteCode getUserInviteCode() {
		return userInviteCode;
	}
}
