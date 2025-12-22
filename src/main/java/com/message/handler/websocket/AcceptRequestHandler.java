package com.message.handler.websocket;

import java.util.Optional;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.AcceptRequest;
import com.message.dto.websocket.outbound.AcceptNotification;
import com.message.dto.websocket.outbound.AcceptResponse;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.service.UserConnectionService;
import com.message.session.WebSocketSessionManager;

@Component
public class AcceptRequestHandler implements BaseRequestHandler<AcceptRequest> {

	private final UserConnectionService userConnectionService;
	private final WebSocketSessionManager webSocketSessionManager;

	public AcceptRequestHandler(UserConnectionService userConnectionService,
		WebSocketSessionManager webSocketSessionManager) {
		this.userConnectionService = userConnectionService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, AcceptRequest request) {
		UserId accepterUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		Pair<Optional<UserId>, String> result =
			userConnectionService.accept(accepterUserId, request.getUsername());

		result.getFirst().ifPresentOrElse(inviterUserId -> {
			webSocketSessionManager.sendMessage(
				senderSession, new AcceptResponse(request.getUsername()));

			String accepterUsername = result.getSecond();
			webSocketSessionManager.sendMessage(
				webSocketSessionManager.getSession(inviterUserId), new AcceptNotification(accepterUsername));
		}, () -> {
			String errorMessage = result.getSecond();
			webSocketSessionManager.sendMessage(
				senderSession, new ErrorResponse(MessageType.ACCEPT_REQUEST, errorMessage));
		});
	}
}
