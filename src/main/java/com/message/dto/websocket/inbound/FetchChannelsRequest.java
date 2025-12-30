package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.message.constant.MessageType;

public class FetchChannelsRequest extends BaseRequest {

	@JsonCreator
	public FetchChannelsRequest() {
		super(MessageType.FETCH_CHANNELS_REQUEST);
	}
}
