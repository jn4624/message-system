package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.FetchChannelsRequest;
import com.message.dto.websocket.outbound.FetchChannelsResponse;
import com.message.service.ChannelService;
import com.message.session.WebSocketSessionManager;

@Component
public class FetchChannelsRequestHandler implements BaseRequestHandler<FetchChannelsRequest> {

	private final ChannelService channelService;
	private final WebSocketSessionManager webSocketSessionManager;

	public FetchChannelsRequestHandler(ChannelService channelService, WebSocketSessionManager webSocketSessionManager) {
		this.channelService = channelService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, FetchChannelsRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		webSocketSessionManager.sendMessage(
			senderSession, new FetchChannelsResponse(channelService.getChannels(senderUserId)));
	}
}
