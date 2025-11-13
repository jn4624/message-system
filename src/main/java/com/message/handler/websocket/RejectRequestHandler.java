package com.message.handler.websocket;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.Constants;
import com.message.constant.MessageType;
import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.RejectRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.RejectResponse;
import com.message.service.UserConnectionService;
import com.message.session.WebSocketSessionManager;

@Component
public class RejectRequestHandler implements BaseRequestHandler<RejectRequest> {

	private final UserConnectionService userConnectionService;
	private final WebSocketSessionManager webSocketSessionManager;

	public RejectRequestHandler(UserConnectionService userConnectionService,
		WebSocketSessionManager webSocketSessionManager) {
		this.userConnectionService = userConnectionService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, RejectRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(Constants.USER_ID.getValue());
		Pair<Boolean, String> result = userConnectionService.reject(senderUserId, request.getUsername());

		if (result.getFirst()) {
			webSocketSessionManager.sendMessage(senderSession,
				new RejectResponse(request.getUsername(), UserConnectionStatus.REJECTED));
		} else {
			String errorMessage = result.getSecond();
			webSocketSessionManager.sendMessage(senderSession,
				new ErrorResponse(MessageType.REJECT_REQUEST, errorMessage));
		}
	}
}
