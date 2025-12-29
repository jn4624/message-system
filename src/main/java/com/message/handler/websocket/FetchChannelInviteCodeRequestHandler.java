package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.FetchChannelInviteCodeRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.FetchChannelInviteCodeResponse;
import com.message.service.ChannelService;
import com.message.session.WebSocketSessionManager;

@Component
public class FetchChannelInviteCodeRequestHandler implements BaseRequestHandler<FetchChannelInviteCodeRequest> {

	private final ChannelService channelService;
	private final WebSocketSessionManager webSocketSessionManager;

	public FetchChannelInviteCodeRequestHandler(
		ChannelService channelService,
		WebSocketSessionManager webSocketSessionManager
	) {
		this.channelService = channelService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, FetchChannelInviteCodeRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		if (!channelService.isJoined(request.getChannelId(), senderUserId)) {
			webSocketSessionManager.sendMessage(
				senderSession,
				new ErrorResponse(MessageType.FETCH_CHANNEL_INVITE_CODE_REQUEST, "Not joined the channel."));
			return;
		}

		channelService.getInviteCode(request.getChannelId())
			.ifPresentOrElse(inviteCode ->
					webSocketSessionManager.sendMessage(
						senderSession, new FetchChannelInviteCodeResponse(request.getChannelId(), inviteCode)),
				() -> webSocketSessionManager.sendMessage(
					senderSession, new ErrorResponse(
						MessageType.FETCH_CHANNEL_INVITE_CODE_REQUEST, "Fetch channel invite code failed.")));
	}
}
