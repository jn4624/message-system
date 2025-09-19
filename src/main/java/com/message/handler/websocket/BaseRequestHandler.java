package com.message.handler.websocket;

import org.springframework.web.socket.WebSocketSession;

import com.message.dto.websocket.inbound.BaseRequest;

public interface BaseRequestHandler<T extends BaseRequest> {

	void handleRequest(WebSocketSession webSocketSession, T request);
}
