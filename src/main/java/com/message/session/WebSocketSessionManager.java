package com.message.session;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.message.dto.domain.UserId;

@Component
public class WebSocketSessionManager {

	private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);
	// 멀티 스레드가 접근할 예정이라 ConcurrentHashMap 사용
	private final Map<UserId, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public List<WebSocketSession> getSessions() {
		return sessions.values().stream().toList();
	}

	public WebSocketSession getSession(UserId userId) {
		return sessions.get(userId);
	}

	public void putSession(UserId userId, WebSocketSession webSocketSession) {
		log.info("Store Session: {}", webSocketSession.getId());
		sessions.put(userId, webSocketSession);
	}

	public void closeSession(UserId userId) {
		try {
			WebSocketSession webSocketSession = sessions.remove(userId);
			if (webSocketSession != null) {
				log.info("Remove session: {}", userId);
				webSocketSession.close();
				log.info("Close session: {}", userId);
			}
		} catch (Exception e) {
			log.error("Failed WebSocketSession close. userId: {}", userId);
		}
	}

	public void sendMessage(WebSocketSession session, String message) throws IOException {
		try {
			session.sendMessage(new TextMessage(message));
			log.info("send message: {} to {}", message, session.getId());
		} catch (IOException e) {
			log.error("Send message failed. cause: {}", e.getMessage());
			throw e;
		}
	}
}
