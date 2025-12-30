package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.ResultType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.QuitRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.QuitResponse;
import com.message.service.ChannelService;
import com.message.session.WebSocketSessionManager;

@Component
public class QuitRequestHandler implements BaseRequestHandler<QuitRequest> {

	private final ChannelService channelService;
	private final WebSocketSessionManager webSocketSessionManager;

	public QuitRequestHandler(
		ChannelService channelService,
		WebSocketSessionManager webSocketSessionManager
	) {
		this.channelService = channelService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, QuitRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		ResultType result;

		try {
			result = channelService.quit(request.getChannelId(), senderUserId);
		} catch (Exception e) {
			webSocketSessionManager.sendMessage(
				senderSession, new ErrorResponse(MessageType.QUIT_REQUEST, ResultType.FAILED.getMessage()));
			return;
		}

		if (result == ResultType.SUCCESS) {
			webSocketSessionManager.sendMessage(
				senderSession, new QuitResponse(request.getChannelId()));
		} else {
			webSocketSessionManager.sendMessage(
				senderSession, new ErrorResponse(MessageType.QUIT_REQUEST, result.getMessage()));
		}
	}
}
