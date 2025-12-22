package com.message.dto.websocket.outbound;

import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;

public class EnterResponse extends BaseMessage {

	private final ChannelId channelId;
	private final String title;

	public EnterResponse(ChannelId channelId, String title) {
		super(MessageType.ENTER_RESPONSE);
		this.channelId = channelId;
		this.title = title;
	}

	public ChannelId getChannelId() {
		return channelId;
	}

	public String getTitle() {
		return title;
	}
}
