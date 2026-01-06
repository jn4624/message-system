package com.message.handler.websocket;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.dto.domain.Connection;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.FetchConnectionsRequest;
import com.message.dto.websocket.outbound.FetchConnectionsResponse;
import com.message.service.ClientNotificationService;
import com.message.service.UserConnectionService;

@Component
public class FetchConnectionsRequestHandler implements BaseRequestHandler<FetchConnectionsRequest> {

	private final UserConnectionService userConnectionService;
	private final ClientNotificationService clientNotificationService;

	public FetchConnectionsRequestHandler(
		UserConnectionService userConnectionService,
		ClientNotificationService clientNotificationService
	) {
		this.userConnectionService = userConnectionService;
		this.clientNotificationService = clientNotificationService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, FetchConnectionsRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());
		List<Connection> connections = userConnectionService.getUsersByStatus(senderUserId, request.getStatus())
			.stream().map(user -> new Connection(user.username(), request.getStatus())).toList();

		clientNotificationService.sendMessage(senderSession, senderUserId, new FetchConnectionsResponse(connections));
	}
}
