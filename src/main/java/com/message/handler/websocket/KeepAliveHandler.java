package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.KeepAlive;
import com.message.service.SessionService;

@Component
public class KeepAliveHandler implements BaseRequestHandler<KeepAlive> {

	private final SessionService sessionService;

	public KeepAliveHandler(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, KeepAlive keepAlive) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());
		sessionService.refreshTTL(
			senderUserId, (String)senderSession.getAttributes().get(IdKey.HTTP_SESSION_ID.getValue()));
	}
}
