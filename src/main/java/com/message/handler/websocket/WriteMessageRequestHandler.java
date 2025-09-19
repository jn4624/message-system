package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.dto.domain.Message;
import com.message.dto.websocket.inbound.WriteMessageRequest;
import com.message.entity.MessageEntity;
import com.message.repository.MessageRepository;
import com.message.session.WebSocketSessionManager;

@Component
public class WriteMessageRequestHandler implements BaseRequestHandler<WriteMessageRequest> {

	private final WebSocketSessionManager webSocketSessionManager;
	private final MessageRepository messageRepository;

	public WriteMessageRequestHandler(WebSocketSessionManager webSocketSessionManager,
		MessageRepository messageRepository) {
		this.webSocketSessionManager = webSocketSessionManager;
		this.messageRepository = messageRepository;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, WriteMessageRequest request) {
		Message receivedMessage = new Message(request.getUsername(),
			request.getContent());

		// 데이터베이스 저장
		messageRepository.save(new MessageEntity(receivedMessage.username(), receivedMessage.content()));

		webSocketSessionManager.getSessions().forEach(participantSession -> {
			if (!senderSession.getId().equals(participantSession.getId())) {
				webSocketSessionManager.sendMessage(participantSession, receivedMessage);
			}
		});
	}
}
