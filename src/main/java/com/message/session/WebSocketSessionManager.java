package com.message.session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class WebSocketSessionManager {

	private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);
	// 멀티 스레드가 접근할 예정이라 ConcurrentHashMap 사용
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public List<WebSocketSession> getSessions() {
		return sessions.values().stream().toList();
	}

	public void storeSession(WebSocketSession webSocketSession) {
		log.info("Store Session: {}", webSocketSession.getId());
		sessions.put(webSocketSession.getId(), webSocketSession);
	}

	public void terminateSession(String sessionId) {
		try {
			WebSocketSession webSocketSession = sessions.remove(sessionId);
			if (webSocketSession != null) {
				log.info("Remove session: {}", sessionId);
				webSocketSession.close();
				log.info("Close session: {}", sessionId);
			}
		} catch (Exception e) {
			log.error("Failed WebSocketSession close. sessionId: {}", sessionId);
		}
	}
}
