package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.Constants;
import com.message.dto.websocket.inbound.KeepAliveRequest;
import com.message.service.SessionService;

@Component
public class KeepAliveRequestHandler implements BaseRequestHandler<KeepAliveRequest> {

	private final SessionService sessionService;

	public KeepAliveRequestHandler(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, KeepAliveRequest request) {
		sessionService.refreshTTL(
			(String)senderSession.getAttributes().get(Constants.HTTP_SESSION_ID.getValue()));
	}
}
