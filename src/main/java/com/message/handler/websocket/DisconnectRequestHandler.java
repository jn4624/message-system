package com.message.handler.websocket;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.DisconnectRequest;
import com.message.dto.websocket.outbound.DisconnectResponse;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.service.ClientNotificationService;
import com.message.service.UserConnectionService;

@Component
public class DisconnectRequestHandler implements BaseRequestHandler<DisconnectRequest> {

	private final UserConnectionService userConnectionService;
	private final ClientNotificationService clientNotificationService;

	public DisconnectRequestHandler(
		UserConnectionService userConnectionService,
		ClientNotificationService clientNotificationService
	) {
		this.userConnectionService = userConnectionService;
		this.clientNotificationService = clientNotificationService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, DisconnectRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());
		Pair<Boolean, String> result = userConnectionService.disconnect(senderUserId, request.getUsername());

		if (result.getFirst()) {
			clientNotificationService.sendMessage(
				senderSession, senderUserId, new DisconnectResponse(
					request.getUsername(), UserConnectionStatus.DISCONNECTED));
		} else {
			String errorMessage = result.getSecond();
			clientNotificationService.sendMessage(
				senderSession, senderUserId, new ErrorResponse(MessageType.DISCONNECT_REQUEST, errorMessage));
		}
	}
}
