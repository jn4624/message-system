package com.message.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import com.message.constant.IdKey;
import com.message.constant.KeyPrefix;
import com.message.dto.domain.ChannelId;
import com.message.dto.domain.UserId;

@Service
public class SessionService {

	private static final Logger log = LoggerFactory.getLogger(SessionService.class);

	private final SessionRepository<? extends Session> httpSessionRepository;
	private final CacheService cacheService;
	private final long TTL = 300;

	public SessionService(
		SessionRepository<? extends Session> httpSessionRepository,
		CacheService cacheService
	) {
		this.httpSessionRepository = httpSessionRepository;
		this.cacheService = cacheService;
	}

	public String getUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getName();
	}

	public List<UserId> getOnlineParticipantUserIds(ChannelId channelId, List<UserId> userIds) {
		List<String> channelIdKeys = userIds.stream().map(this::buildChannelIdKey).toList();
		List<String> channelIds = cacheService.get(channelIdKeys);

		if (channelIds != null) {
			List<UserId> onlineParticipantUserIds = new ArrayList<>(channelIds.size());
			for (int idx = 0; idx < userIds.size(); idx++) {
				String value = channelIds.get(idx);
					/*
					  Null 체크를 하는 이유
					  - channelIdKeys에 해당하는 Redis의 데이터가 5개 존재해도
					    조건으로 넘긴 channelIdKeys의 개수가 10개라면 나머지 5개는 null로 채워져 조회되기 때문에
					  - 단 데이터의 순서는 channelIdKeys의 순서대로 조회된다
					 */
				onlineParticipantUserIds.add(
					(value != null && value.equals(channelId.id().toString())) ? userIds.get(idx) : null);
			}

			return onlineParticipantUserIds;
		}

		return Collections.emptyList();
	}

	public boolean setActiveChannel(UserId userId, ChannelId channelId) {
		return cacheService.set(buildChannelIdKey(userId), channelId.id().toString(), TTL);
	}

	public boolean removeActiveChannel(UserId userId) {
		return cacheService.delete(buildChannelIdKey(userId));
	}

	public void refreshTTL(UserId userId, String httpSessionId) {
		String channelIdKey = buildChannelIdKey(userId);

		try {
			Session httpSession = httpSessionRepository.findById(httpSessionId);

			if (httpSession != null) {
			/*
			  - 세션의 마지막 접근 시간을 현재 시간으로 갱신하여 TTL 연장
			  - 아래 설정으로는 저장이 되지 않는다.
			    제네릭 제약사항은 읽기 전용이라 httpSessionRepository의 save를 호출할 수 없다.
			  - 따라서 RedisSessionConfig의 설정을 변경하는 방법으로 진행한다.
			 */
				httpSession.setLastAccessedTime(Instant.now());
				cacheService.expire(channelIdKey, TTL);
			}
		} catch (Exception e) {
			log.error("Redis find failed. httpSessionId: {}, cause: {}", httpSessionId, e.getMessage());
		}
	}

	private String buildChannelIdKey(UserId userId) {
		return cacheService.buildKey(KeyPrefix.USER, userId.id().toString(), IdKey.CHANNEL_ID.getValue());
	}
}
