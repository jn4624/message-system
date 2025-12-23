package com.message.dto.websocket.outbound;

import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;

public class MessageNotification extends BaseMessage {

	private final ChannelId channelId;
	private final String username;
	private final String content;

	public MessageNotification(ChannelId channelId, String username, String content) {
		super(MessageType.NOTIFY_MESSAGE);
		this.channelId = channelId;
		this.username = username;
		this.content = content;
	}

	public ChannelId getChannelId() {
		return channelId;
	}

	public String getUsername() {
		return username;
	}

	public String getContent() {
		return content;
	}
}
