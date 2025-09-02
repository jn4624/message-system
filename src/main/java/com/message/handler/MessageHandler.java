package com.message.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.message.dto.Message;

@Component
public class MessageHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private WebSocketSession leftSide = null;
	private WebSocketSession rightSide = null;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.info("ConnectionEstablished: {}", session.getId());

		if (leftSide == null) {
			leftSide = session;
			return;
		} else if (rightSide == null) {
			rightSide = session;
			return;
		}

		log.warn("빈 자리 없음. {}의 접속 거부.", session.getId());
		session.close();
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
		log.info("ConnectionClosed: [{}] from {}", status, session.getId());

		if (leftSide == session) {
			leftSide = null;
		} else if (rightSide == session) {
			rightSide = null;
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, @NonNull TextMessage message) {
		log.info("Received TextMessage: [{}] from {}", message, session.getId());

		String payload = message.getPayload();
		try {
			Message receivedMessage = objectMapper.readValue(payload, Message.class);
			if (leftSide == session) {
				sendMessage(rightSide, receivedMessage.content());
			} else if (rightSide == session) {
				sendMessage(leftSide, receivedMessage.content());
			}
		} catch (Exception e) {
			String errorMessage = "유효한 프로토콜이 아닙니다.";
			log.error("errorMessage payload: {} from {}", payload, session.getId());
			sendMessage(session, errorMessage);
		}
	}

	private void sendMessage(WebSocketSession session, String message) {
		try {
			// String msg = objectMapper.writeValueAsString(new Message(message));
			session.sendMessage(new TextMessage(message));
			log.info("send message: {} to {}", message, session.getId());
		} catch (Exception e) {
			log.error("메시지 전송 실패 to {} error: {}", session.getId(), e.getMessage());
		}
	}
}
