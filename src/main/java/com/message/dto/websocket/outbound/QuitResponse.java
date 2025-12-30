package com.message.dto.websocket.outbound;

import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;

public class QuitResponse extends BaseMessage {

	private final ChannelId channelId;

	public QuitResponse(ChannelId channelId) {
		super(MessageType.QUIT_RESPONSE);
		this.channelId = channelId;
	}

	public ChannelId getChannelId() {
		return channelId;
	}
}
