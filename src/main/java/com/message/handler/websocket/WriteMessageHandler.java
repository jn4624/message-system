package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.dto.domain.ChannelId;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.WriteMessage;
import com.message.dto.websocket.outbound.MessageNotification;
import com.message.service.MessageService;
import com.message.service.UserService;
import com.message.session.WebSocketSessionManager;

@Component
public class WriteMessageHandler implements BaseRequestHandler<WriteMessage> {

	private final UserService userService;
	private final MessageService messageService;
	private final WebSocketSessionManager webSocketSessionManager;

	public WriteMessageHandler(
		UserService userService,
		MessageService messageService,
		WebSocketSessionManager webSocketSessionManager
	) {
		this.userService = userService;
		this.messageService = messageService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, WriteMessage writeMessage) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());
		ChannelId channelId = writeMessage.getChannelId();
		String content = writeMessage.getContent();
		String senderUsername = userService.getUsername(senderUserId).orElse("unknown");

		/*
		  - 아래 부분이 실제로 메시지를 보내는 I/O가 발생하는 부분
		 */
		messageService.sendMessage(senderUserId, content, channelId, (participantId) -> {
			WebSocketSession participantSession = webSocketSessionManager.getSession(participantId);
			MessageNotification messageNotification = new MessageNotification(channelId, senderUsername, content);
			if (participantSession != null) {
				webSocketSessionManager.sendMessage(participantSession, messageNotification);
			}
		});
	}
}
