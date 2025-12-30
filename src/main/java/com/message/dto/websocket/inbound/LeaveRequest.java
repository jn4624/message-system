package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.message.constant.MessageType;

public class LeaveRequest extends BaseRequest {

	@JsonCreator
	public LeaveRequest() {
		super(MessageType.LEAVE_REQUEST);
	}
}
