package com.message.dto.websocket.outbound;

import com.message.constant.MessageType;

public class LeaveResponse extends BaseMessage {

	public LeaveResponse() {
		super(MessageType.LEAVE_RESPONSE);
	}
}
