package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.message.constant.MessageType;

public class KeepAlive extends BaseRequest {

	@JsonCreator
	public KeepAlive() {
		super(MessageType.KEEP_ALIVE);
	}
}
