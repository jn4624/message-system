package com.message.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.message.auth.WebSocketHttpSessionHandshakeInterceptor;
import com.message.handler.MessageHandler;

@Configuration
@EnableWebSocket
public class WebSocketHandlerConfig implements WebSocketConfigurer {

	private final MessageHandler messageHandler;
	private final WebSocketHttpSessionHandshakeInterceptor webSocketHttpSessionHandshakeInterceptor;

	public WebSocketHandlerConfig(MessageHandler messageHandler,
		WebSocketHttpSessionHandshakeInterceptor webSocketHttpSessionHandshakeInterceptor) {
		this.messageHandler = messageHandler;
		this.webSocketHttpSessionHandshakeInterceptor = webSocketHttpSessionHandshakeInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(messageHandler, "/ws/v1/message")
			.addInterceptors(webSocketHttpSessionHandshakeInterceptor);
	}
}
