package com.message.service;

import java.util.List;
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

	private final ChannelService channelService;
	private final MessageRepository messageRepository;

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

		List<UserId> participantIds = channelService.getParticipantIds(channelId);
		participantIds.stream()
			.filter(userId -> !userId.equals(sendUserId))
			.forEach(participantId -> {
				if (channelService.isOnline(participantId, channelId)) {
					messageSender.accept(participantId);
				}
			});
	}
}
