package com.message.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.message.constant.ResultType;
import com.message.dto.domain.Channel;
import com.message.dto.domain.ChannelId;
import com.message.dto.domain.UserId;
import com.message.dto.projection.ChannelTitleProjection;
import com.message.entity.ChannelEntity;
import com.message.entity.UserChannelEntity;
import com.message.repository.ChannelRepository;
import com.message.repository.UserChannelRepository;

@Service
public class ChannelService {

	private static final Logger log = LoggerFactory.getLogger(ChannelService.class);

	private final SessionService sessionService;
	private final ChannelRepository channelRepository;
	private final UserChannelRepository userChannelRepository;

	public ChannelService(
		SessionService sessionService,
		ChannelRepository channelRepository,
		UserChannelRepository userChannelRepository
	) {
		this.sessionService = sessionService;
		this.channelRepository = channelRepository;
		this.userChannelRepository = userChannelRepository;
	}

	public boolean isJoined(ChannelId channelId, UserId userId) {
		return userChannelRepository.existsByUserIdAndChannelId(userId.id(), channelId.id());
	}

	public List<UserId> getParticipantIds(ChannelId channelId) {
		return userChannelRepository.findUserIdsByChannelId(channelId.id())
			.stream()
			.map(userId -> new UserId(userId.getUserId()))
			.toList();
	}

	public boolean isOnline(UserId userId, ChannelId channelId) {
		return sessionService.isOnline(userId, channelId);
	}

	@Transactional
	public Pair<Optional<Channel>, ResultType> create(UserId senderUserId, UserId participantId, String title) {
		if (title == null || title.isEmpty()) {
			log.warn("Invalid args : title is empty");
			return Pair.of(Optional.empty(), ResultType.INVALID_ARGS);
		}

		try {
			final int HEAD_COUNT = 2;
			ChannelEntity channelEntity = channelRepository.save(new ChannelEntity(title, HEAD_COUNT));
			Long channelId = channelEntity.getChannelId();

			List<UserChannelEntity> userChannelEntities =
				List.of(new UserChannelEntity(senderUserId.id(), channelId, 0),
					new UserChannelEntity(participantId.id(), channelId, 0));
			userChannelRepository.saveAll(userChannelEntities);

			Channel channel = new Channel(new ChannelId(channelId), title, HEAD_COUNT);
			return Pair.of(Optional.of(channel), ResultType.SUCCESS);
		} catch (Exception e) {
			log.error("Create failed. cause: {}", e.getMessage());
			throw e;
		}
	}

	public Pair<Optional<String>, ResultType> enter(ChannelId channelId, UserId userId) {
		if (!isJoined(channelId, userId)) {
			log.warn("Enter channel failed. User not joined the channel. channelId: {}, userId: {}", channelId, userId);
			return Pair.of(Optional.empty(), ResultType.NOT_JOINED);
		}

		Optional<String> title = channelRepository.findChannelTitleByChannelId(channelId.id())
			.map(ChannelTitleProjection::getTitle);
		if (title.isEmpty()) {
			log.warn("Enter channel failed. Channel does not exist. channelId: {}, userId: {}", channelId, userId);
			return Pair.of(Optional.empty(), ResultType.NOT_FOUND);
		}

		if (sessionService.setActiveChannel(userId, channelId)) {
			return Pair.of(title, ResultType.SUCCESS);
		}

		log.error("Enter channel failed. channelId: {}, userId: {}", channelId, userId);
		return Pair.of(Optional.empty(), ResultType.FAILED);
	}
}
