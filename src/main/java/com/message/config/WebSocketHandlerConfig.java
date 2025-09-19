package com.message.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.message.auth.WebSocketHttpSessionHandshakeInterceptor;
import com.message.handler.WebsocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketHandlerConfig implements WebSocketConfigurer {

	private final WebsocketHandler websocketHandler;
	private final WebSocketHttpSessionHandshakeInterceptor webSocketHttpSessionHandshakeInterceptor;

	public WebSocketHandlerConfig(WebsocketHandler websocketHandler,
		WebSocketHttpSessionHandshakeInterceptor webSocketHttpSessionHandshakeInterceptor) {
		this.websocketHandler = websocketHandler;
		this.webSocketHttpSessionHandshakeInterceptor = webSocketHttpSessionHandshakeInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(websocketHandler, "/ws/v1/message")
			.addInterceptors(webSocketHttpSessionHandshakeInterceptor);
	}
}
