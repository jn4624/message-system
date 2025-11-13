package com.message.handler.websocket;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.Constants;
import com.message.dto.domain.Connection;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.FetchConnectionsRequest;
import com.message.dto.websocket.outbound.FetchConnectionsResponse;
import com.message.service.UserConnectionService;
import com.message.session.WebSocketSessionManager;

@Component
public class FetchConnectionsRequestHandler implements BaseRequestHandler<FetchConnectionsRequest> {

	private final UserConnectionService userConnectionService;
	private final WebSocketSessionManager webSocketSessionManager;

	public FetchConnectionsRequestHandler(UserConnectionService userConnectionService,
		WebSocketSessionManager webSocketSessionManager) {
		this.userConnectionService = userConnectionService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, FetchConnectionsRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(Constants.USER_ID.getValue());
		List<Connection> connections = userConnectionService.getUsersByStatus(senderUserId, request.getStatus())
			.stream().map(user -> new Connection(user.username(), request.getStatus())).toList();

		webSocketSessionManager.sendMessage(senderSession, new FetchConnectionsResponse(connections));
	}
}
