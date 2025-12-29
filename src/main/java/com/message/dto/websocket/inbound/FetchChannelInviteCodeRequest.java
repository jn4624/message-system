package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;

public class FetchChannelInviteCodeRequest extends BaseRequest {

	private final ChannelId channelId;

	@JsonCreator
	public FetchChannelInviteCodeRequest(@JsonProperty("channelId") ChannelId channelId) {
		super(MessageType.FETCH_CHANNEL_INVITE_CODE_REQUEST);
		this.channelId = channelId;
	}

	public ChannelId getChannelId() {
		return channelId;
	}
}
