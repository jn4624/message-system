package com.message.handler.websocket;

import java.util.Optional;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.ResultType;
import com.message.dto.domain.Channel;
import com.message.dto.domain.ChannelId;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.JoinRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.JoinResponse;
import com.message.service.ChannelService;
import com.message.session.WebSocketSessionManager;

@Component
public class JoinRequestHandler implements BaseRequestHandler<JoinRequest> {

	private final ChannelService channelService;
	private final WebSocketSessionManager webSocketSessionManager;

	public JoinRequestHandler(
		ChannelService channelService,
		WebSocketSessionManager webSocketSessionManager
	) {
		this.channelService = channelService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, JoinRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		Pair<Optional<Channel>, ResultType> result;

		try {
			result = channelService.join(request.getInviteCode(), senderUserId);
		} catch (Exception e) {
			webSocketSessionManager.sendMessage(
				senderSession, new ErrorResponse(MessageType.JOIN_REQUEST, ResultType.FAILED.getMessage()));
			return;
		}

		result.getFirst().ifPresentOrElse(channel ->
				webSocketSessionManager.sendMessage(
					senderSession, new JoinResponse(new ChannelId(channel.channelId().id()), channel.title()))
			, () -> webSocketSessionManager.sendMessage(
				senderSession, new ErrorResponse(MessageType.JOIN_REQUEST, result.getSecond().getMessage())));
	}
}
