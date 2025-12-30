package com.message.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.message.constant.ResultType;
import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.Channel;
import com.message.dto.domain.ChannelId;
import com.message.dto.domain.InviteCode;
import com.message.dto.domain.UserId;
import com.message.dto.projection.ChannelTitleProjection;
import com.message.entity.ChannelEntity;
import com.message.entity.UserChannelEntity;
import com.message.repository.ChannelRepository;
import com.message.repository.UserChannelRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ChannelService {

	private static final Logger log = LoggerFactory.getLogger(ChannelService.class);
	private static final int LIMIT_HEAD_COUNT = 100;

	private final SessionService sessionService;
	private final UserConnectionService userConnectionService;
	private final ChannelRepository channelRepository;
	private final UserChannelRepository userChannelRepository;

	public ChannelService(
		SessionService sessionService,
		UserConnectionService userConnectionService,
		ChannelRepository channelRepository,
		UserChannelRepository userChannelRepository
	) {
		this.sessionService = sessionService;
		this.userConnectionService = userConnectionService;
		this.channelRepository = channelRepository;
		this.userChannelRepository = userChannelRepository;
	}

	public Optional<InviteCode> getInviteCode(ChannelId channelId) {
		Optional<InviteCode> inviteCode = channelRepository.findChannelInviteCodeByChannelId(channelId.id())
			.map(inviteCodeProjection -> new InviteCode(inviteCodeProjection.getInviteCode()));

		if (inviteCode.isEmpty()) {
			log.warn("Invite code is not exist. channelId: {}", channelId);
		}

		return inviteCode;
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

	public List<UserId> getOnlineParticipantIds(ChannelId channelId) {
		return sessionService.getOnlineParticipantUserIds(channelId, getParticipantIds(channelId));
	}

	public Optional<Channel> getChannel(InviteCode inviteCode) {
		return channelRepository.findChannelByInviteCode(inviteCode.code())
			.map(projection -> new Channel(
				new ChannelId(projection.getChannelId()), projection.getTitle(), projection.getHeadCount()));
	}

	public List<Channel> getChannels(UserId userId) {
		return userChannelRepository.findChannelsByUserId(userId.id())
			.stream()
			.map(projection -> new Channel(
				new ChannelId(projection.getChannelId()), projection.getTitle(), projection.getHeadCount()))
			.toList();
	}

	@Transactional
	public Pair<Optional<Channel>, ResultType> create(UserId senderUserId, List<UserId> participantIds, String title) {
		if (title == null || title.isEmpty()) {
			log.warn("Invalid args : title is empty");
			return Pair.of(Optional.empty(), ResultType.INVALID_ARGS);
		}

		int headCount = participantIds.size() + 1; // 나 자신도 포함
		if (headCount > LIMIT_HEAD_COUNT) {
			log.warn("Over limit channel. senderUserId: {}, participantIds count: {}, title: {}",
				senderUserId, participantIds.size(), title);
			return Pair.of(Optional.empty(), ResultType.OVER_LIMIT);
		}

		if (userConnectionService.countConnectionStatus(
			senderUserId, participantIds, UserConnectionStatus.ACCEPTED) != participantIds.size()) {
			log.warn("Included unconnected user. participantId: {}", participantIds);
			return Pair.of(Optional.empty(), ResultType.NOT_ALLOWED);
		}

		try {
			ChannelEntity channelEntity = channelRepository.save(new ChannelEntity(title, headCount));
			Long channelId = channelEntity.getChannelId();

			List<UserChannelEntity> userChannelEntities = participantIds
				.stream()
				.map(participantUserId ->
					new UserChannelEntity(participantUserId.id(), channelId, 0))
				.collect(Collectors.toList());
			userChannelEntities.add(new UserChannelEntity(senderUserId.id(), channelId, 0));
			userChannelRepository.saveAll(userChannelEntities);

			Channel channel = new Channel(new ChannelId(channelId), title, headCount);
			return Pair.of(Optional.of(channel), ResultType.SUCCESS);
		} catch (Exception e) {
			log.error("Create failed. cause: {}", e.getMessage());
			throw e;
		}
	}

	@Transactional
	public Pair<Optional<Channel>, ResultType> join(InviteCode inviteCode, UserId userId) {
		Optional<Channel> ch = getChannel(inviteCode);
		if (ch.isEmpty()) {
			return Pair.of(Optional.empty(), ResultType.NOT_FOUND);
		}

		Channel channel = ch.get();
		if (isJoined(channel.channelId(), userId)) {
			return Pair.of(Optional.empty(), ResultType.ALREADY_JOINED);
		} else if (channel.headCount() >= LIMIT_HEAD_COUNT) {
			return Pair.of(Optional.empty(), ResultType.OVER_LIMIT);
		}

		ChannelEntity channelEntity = channelRepository.findForUpdateByChannelId(channel.channelId().id())
			.orElseThrow(() -> new EntityNotFoundException("Invalid channelId: " + channel.channelId().id()));

		if (channelEntity.getHeadCount() < LIMIT_HEAD_COUNT) {
			channelEntity.setHeadCount(channelEntity.getHeadCount() + 1);
			userChannelRepository.save(new UserChannelEntity(userId.id(), channel.channelId().id(), 0));
		}

		return Pair.of(Optional.of(channel), ResultType.SUCCESS);
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
