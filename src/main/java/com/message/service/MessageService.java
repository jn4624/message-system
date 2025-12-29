package com.message.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.message.dto.domain.ChannelId;
import com.message.dto.domain.UserId;
import com.message.entity.MessageEntity;
import com.message.repository.MessageRepository;

@Service
public class MessageService {

	private static final Logger log = LoggerFactory.getLogger(MessageService.class);
	private static final int THREAD_POOL_SIZE = 10;

	private final ChannelService channelService;
	private final MessageRepository messageRepository;
	private final ExecutorService senderThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	public MessageService(ChannelService channelService, MessageRepository messageRepository) {
		this.channelService = channelService;
		this.messageRepository = messageRepository;
	}

	public void sendMessage(UserId sendUserId, String content, ChannelId channelId, Consumer<UserId> messageSender) {
		try {
			// 데이터베이스 저장
			messageRepository.save(new MessageEntity(sendUserId.id(), content));
		} catch (Exception e) {
			log.error("Send message failed. cause: {}", e.getMessage());
			return;
		}

		channelService.getOnlineParticipantIds(channelId)
			.stream()
			.filter(participantUserId -> !participantUserId.equals(sendUserId))
			.forEach(
				participantId ->
					CompletableFuture.runAsync(() ->
						messageSender.accept(participantId), senderThreadPool));
	}
}
