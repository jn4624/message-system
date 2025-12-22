package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.dto.websocket.inbound.WriteMessage;
import com.message.dto.websocket.outbound.MessageNotification;
import com.message.entity.MessageEntity;
import com.message.repository.MessageRepository;
import com.message.session.WebSocketSessionManager;

@Component
public class WriteMessageHandler implements BaseRequestHandler<WriteMessage> {

	private final WebSocketSessionManager webSocketSessionManager;
	private final MessageRepository messageRepository;

	public WriteMessageHandler(WebSocketSessionManager webSocketSessionManager,
		MessageRepository messageRepository) {
		this.webSocketSessionManager = webSocketSessionManager;
		this.messageRepository = messageRepository;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, WriteMessage writeMessage) {
		MessageNotification receivedMessage =
			new MessageNotification(writeMessage.getUsername(), writeMessage.getContent());

		// 데이터베이스 저장
		messageRepository.save(new MessageEntity(receivedMessage.getUsername(), receivedMessage.getContent()));

		webSocketSessionManager.getSessions().forEach(participantSession -> {
			if (!senderSession.getId().equals(participantSession.getId())) {
				webSocketSessionManager.sendMessage(participantSession, receivedMessage);
			}
		});
	}
}
