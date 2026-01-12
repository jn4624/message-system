package com.message.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.MessageType;
import com.message.dto.domain.ChannelId;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.outbound.BaseMessage;
import com.message.entity.MessageEntity;
import com.message.repository.MessageRepository;
import com.message.session.WebSocketSessionManager;
import com.message.util.JsonUtil;

@Service
public class MessageService {

	private static final Logger log = LoggerFactory.getLogger(MessageService.class);
	private static final int THREAD_POOL_SIZE = 10;

	private final ChannelService channelService;
	private final PushService pushService;
	private final WebSocketSessionManager webSocketSessionManager;
	private final MessageRepository messageRepository;
	private final JsonUtil jsonUtil;
	private final ExecutorService senderThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	public MessageService(
		ChannelService channelService,
		PushService pushService,
		WebSocketSessionManager webSocketSessionManager,
		MessageRepository messageRepository,
		JsonUtil jsonUtil
	) {
		this.channelService = channelService;
		this.pushService = pushService;
		this.webSocketSessionManager = webSocketSessionManager;
		this.messageRepository = messageRepository;
		this.jsonUtil = jsonUtil;

		pushService.registerPushMessageType(MessageType.NOTIFY_MESSAGE);
	}

	@Transactional
	public void sendMessage(UserId sendUserId, String content, ChannelId channelId, BaseMessage message) {
		Optional<String> json = jsonUtil.toJson(message);
		if (json.isEmpty()) {
			log.error("Send message failed. messageType: {}", message.getType());
			return;
		}

		String payload = json.get();
		try {
			// 데이터베이스 저장
			messageRepository.save(new MessageEntity(sendUserId.id(), content));
		} catch (Exception e) {
			log.error("Send message failed. cause: {}", e.getMessage());
			return;
		}

		// 오프라인 사용자 분리 처리
		List<UserId> allParticipantIds = channelService.getParticipantIds(channelId);
		List<UserId> onlineParticipantIds = channelService.getOnlineParticipantIds(channelId, allParticipantIds);

		for (int idx = 0; idx < allParticipantIds.size(); idx++) {
			UserId participantId = allParticipantIds.get(idx);

			if (sendUserId.equals(participantId)) {
				continue;
			}

			if (onlineParticipantIds.get(idx) != null) {
				CompletableFuture.runAsync(() -> {
					try {
						/*
						  레디스에서 온라인 사용자로 넘어왔어도 서버에 커넥션이 100% 살아있다고 보장할 수 없다
						  따라서 소켓 세션이 살아 있는지 한번더 체크해줘야 한다
						 */
						WebSocketSession session = webSocketSessionManager.getSession(participantId);
						if (session != null) {
							webSocketSessionManager.sendMessage(session, payload);
						} else {
							pushService.pushMessage(participantId, MessageType.NOTIFY_MESSAGE, payload);
						}
					} catch (Exception e) {
						pushService.pushMessage(participantId, MessageType.NOTIFY_MESSAGE, payload);
					}
				}, senderThreadPool);
			} else {
				pushService.pushMessage(participantId, MessageType.NOTIFY_MESSAGE, payload);
			}
		}
	}
}
