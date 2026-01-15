package com.message.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.message.constant.KeyPrefix;
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
import com.message.util.JsonUtil;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ChannelService {

	private static final Logger log = LoggerFactory.getLogger(ChannelService.class);
	private static final int LIMIT_HEAD_COUNT = 100;

	private final SessionService sessionService;
	private final UserConnectionService userConnectionService;
	private final CacheService cacheService;
	private final ChannelRepository channelRepository;
	private final UserChannelRepository userChannelRepository;
	private final JsonUtil jsonUtil;
	private final long TTL = 600;

	public ChannelService(
		SessionService sessionService,
		UserConnectionService userConnectionService,
		CacheService cacheService,
		ChannelRepository channelRepository,
		UserChannelRepository userChannelRepository,
		JsonUtil jsonUtil
	) {
		this.sessionService = sessionService;
		this.userConnectionService = userConnectionService;
		this.cacheService = cacheService;
		this.channelRepository = channelRepository;
		this.userChannelRepository = userChannelRepository;
		this.jsonUtil = jsonUtil;
	}

	@Transactional(readOnly = true)
	public Optional<InviteCode> getInviteCode(ChannelId channelId) {
		String key = cacheService.buildKey(KeyPrefix.CHANNEL_INVITE_CODE, channelId.id().toString());
		Optional<String> cachedInviteCode = cacheService.get(key);

		if (cachedInviteCode.isPresent()) {
			return Optional.of(new InviteCode(cachedInviteCode.get()));
		}

		Optional<InviteCode> findInviteCode =
			channelRepository.findChannelInviteCodeByChannelId(channelId.id())
				.map(inviteCodeProjection ->
					new InviteCode(inviteCodeProjection.getInviteCode()));

		if (findInviteCode.isEmpty()) {
			log.warn("Invite code is not exist. channelId: {}", channelId);
		}

		findInviteCode.ifPresent(inviteCode -> cacheService.set(key, inviteCode.code(), TTL));
		return findInviteCode;
	}

	@Transactional(readOnly = true)
	public boolean isJoined(ChannelId channelId, UserId userId) {
		String key = cacheService.buildKey(KeyPrefix.JOINED_CHANNEL, channelId.id().toString(), userId.id().toString());
		Optional<String> cachedChannel = cacheService.get(key);

		if (cachedChannel.isPresent()) {
			return true;
		}

		boolean findJoined = userChannelRepository.existsByUserIdAndChannelId(userId.id(), channelId.id());
		if (findJoined) {
			cacheService.set(key, "T", TTL);
		}
		return findJoined;
	}

	@Transactional(readOnly = true)
	public List<UserId> getParticipantIds(ChannelId channelId) {
		String key = cacheService.buildKey(KeyPrefix.PARTICIPANT_IDS, channelId.id().toString());
		Optional<String> cachedParticipantIds = cacheService.get(key);

		if (cachedParticipantIds.isPresent()) {
			return jsonUtil.fromJsonToList(cachedParticipantIds.get(), String.class)
				.stream()
				.map(userId -> new UserId(Long.valueOf(userId)))
				.toList();
		}

		List<UserId> findParticipantIds = userChannelRepository.findUserIdsByChannelId(channelId.id())
			.stream()
			.map(userId -> new UserId(userId.getUserId()))
			.toList();
		if (!findParticipantIds.isEmpty()) {
			jsonUtil.toJson(findParticipantIds
					.stream()
					.map(UserId::id))
				.ifPresent(json -> cacheService.set(key, json, TTL));
		}
		return findParticipantIds;
	}

	public List<UserId> getOnlineParticipantIds(ChannelId channelId, List<UserId> userIds) {
		return sessionService.getOnlineParticipantUserIds(channelId, userIds);
	}

	@Transactional(readOnly = true)
	public Optional<Channel> getChannel(InviteCode inviteCode) {
		String key = cacheService.buildKey(KeyPrefix.CHANNEL, inviteCode.code());
		Optional<String> cachedChannel = cacheService.get(key);

		if (cachedChannel.isPresent()) {
			return jsonUtil.fromJson(cachedChannel.get(), Channel.class);
		}

		Optional<Channel> findChannel = channelRepository.findChannelByInviteCode(inviteCode.code())
			.map(projection -> new Channel(
				new ChannelId(projection.getChannelId()), projection.getTitle(), projection.getHeadCount()));
		findChannel.flatMap(jsonUtil::toJson)
			.ifPresent(json -> cacheService.set(key, json, TTL));
		return findChannel;
	}

	@Transactional(readOnly = true)
	public List<Channel> getChannels(UserId userId) {
		String key = cacheService.buildKey(KeyPrefix.CHANNELS, userId.id().toString());
		Optional<String> cachedChannels = cacheService.get(key);

		if (cachedChannels.isPresent()) {
			return jsonUtil.fromJsonToList(cachedChannels.get(), Channel.class);
		}

		List<Channel> findChannels = userChannelRepository.findChannelsByUserId(userId.id())
			.stream()
			.map(projection -> new Channel(
				new ChannelId(projection.getChannelId()), projection.getTitle(), projection.getHeadCount()))
			.toList();
		if (!findChannels.isEmpty()) {
			jsonUtil.toJson(findChannels)
				.ifPresent(json -> cacheService.set(key, json, TTL));
		}
		return findChannels;
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
			cacheService.delete(List.of(
				cacheService.buildKey(KeyPrefix.CHANNEL, channelEntity.getInviteCode()),
				cacheService.buildKey(KeyPrefix.CHANNELS, userId.id().toString())));
		}

		return Pair.of(Optional.of(channel), ResultType.SUCCESS);
	}

	@Transactional(readOnly = true)
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

	public boolean leave(UserId userId) {
		return sessionService.removeActiveChannel(userId);
	}

	@Transactional
	public ResultType quit(ChannelId channelId, UserId userId) {
		if (!isJoined(channelId, userId)) {
			return ResultType.NOT_JOINED;
		}

		ChannelEntity channelEntity = channelRepository.findForUpdateByChannelId(channelId.id())
			.orElseThrow(() -> new EntityNotFoundException("Invalid channelId: " + channelId));

		if (channelEntity.getHeadCount() > 0) {
			channelEntity.setHeadCount(channelEntity.getHeadCount() - 1);
		} else {
			log.error("Count is already zero. channelId: {}, userId: {}", channelId, userId);
		}

		userChannelRepository.deleteByUserIdAndChannelId(userId.id(), channelId.id());
		cacheService.delete(List.of(
			cacheService.buildKey(KeyPrefix.CHANNEL, channelEntity.getInviteCode()),
			cacheService.buildKey(KeyPrefix.CHANNELS, userId.id().toString())));
		return ResultType.SUCCESS;
	}
}
