package com.message.handler.websocket;

import java.util.Optional;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.ResultType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.EnterRequest;
import com.message.dto.websocket.outbound.EnterResponse;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.service.ChannelService;
import com.message.session.WebSocketSessionManager;

@Component
public class EnterRequestHandler implements BaseRequestHandler<EnterRequest> {

	private final ChannelService channelService;
	private final WebSocketSessionManager webSocketSessionManager;

	public EnterRequestHandler(
		ChannelService channelService,
		WebSocketSessionManager webSocketSessionManager
	) {
		this.channelService = channelService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, EnterRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		Pair<Optional<String>, ResultType> result = channelService.enter(request.getChannelId(), senderUserId);
		result.getFirst().ifPresentOrElse(title ->
				webSocketSessionManager.sendMessage(senderSession, new EnterResponse(request.getChannelId(), title))
			, () -> webSocketSessionManager.sendMessage(
				senderSession, new ErrorResponse(MessageType.ENTER_REQUEST, result.getSecond().getMessage())));
	}
}
