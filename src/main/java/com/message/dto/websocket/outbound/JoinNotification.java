package com.message.dto.websocket.outbound;

import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;

public class JoinNotification extends BaseMessage {

	private final ChannelId channelId;
	private final String title;

	public JoinNotification(ChannelId channelId, String title) {
		super(MessageType.NOTIFY_JOIN);
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
