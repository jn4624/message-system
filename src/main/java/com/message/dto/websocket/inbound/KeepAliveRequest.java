package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.message.constant.MessageType;

public class KeepAliveRequest extends BaseRequest {

	@JsonCreator
	public KeepAliveRequest() {
		super(MessageType.KEEP_ALIVE);
	}
}
