package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;

public class EnterRequest extends BaseRequest {

	private final ChannelId channelId;

	@JsonCreator
	public EnterRequest(@JsonProperty("channelId") ChannelId channelId) {
		super(MessageType.ENTER_REQUEST);
		this.channelId = channelId;
	}

	public ChannelId getChannelId() {
		return channelId;
	}
}
