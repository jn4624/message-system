package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.message.constant.MessageType;

public class FetchUserInviteCodeRequest extends BaseRequest {

	@JsonCreator
	public FetchUserInviteCodeRequest() {
		super(MessageType.FETCH_USER_INVITE_CODE_REQUEST);
	}
}
