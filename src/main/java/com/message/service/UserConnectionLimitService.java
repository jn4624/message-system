package com.message.service;

import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.UserId;
import com.message.entity.UserConnectionEntity;
import com.message.entity.UserEntity;
import com.message.repository.UserConnectionRepository;
import com.message.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserConnectionLimitService {

	private final UserRepository userRepository;
	private final UserConnectionRepository userConnectionRepository;

	/*
	  - 상수로 정의하지 않는 이유는 테스트를 편하기 하기 위해서
	  - final로 정의할 경우 테스트에서 변경이 어렵다.
	 */
	private int limitConnections = 1_000;

	public UserConnectionLimitService(UserRepository userRepository,
		UserConnectionRepository userConnectionRepository) {
		this.userRepository = userRepository;
		this.userConnectionRepository = userConnectionRepository;
	}

	public int getLimitConnections() {
		return limitConnections;
	}

	public void setLimitConnections(int limitConnections) {
		this.limitConnections = limitConnections;
	}

	@Transactional
	public void accept(UserId accepterUserId, UserId inviterUserId) {
		Long firstUserId = Long.min(accepterUserId.id(), inviterUserId.id());
		Long secondUserId = Long.max(accepterUserId.id(), inviterUserId.id());

		UserEntity firstUserEntity = userRepository.findForUpdateByUserId(firstUserId)
			.orElseThrow(() -> new EntityNotFoundException("Invalid userId: " + firstUserId));
		UserEntity secondUserEntity = userRepository.findForUpdateByUserId(secondUserId)
			.orElseThrow(() -> new EntityNotFoundException("Invalid userId: " + secondUserId));

		UserConnectionEntity userConnectionEntity =
			userConnectionRepository.findByPartnerAUserIdAndPartnerBUserIdAndStatus(
					firstUserId, secondUserId, UserConnectionStatus.PENDING)
				.orElseThrow(() -> new EntityNotFoundException("Invalid status"));

		Function<Long, String> getErrorMessage = userId ->
			userId.equals(accepterUserId.id()) ? "Connection limit reached" :
				"Connection limit reached by the other user";

		int firstUserConnectionCount = firstUserEntity.getConnectionCount();
		if (firstUserConnectionCount >= limitConnections) {
			throw new IllegalStateException(getErrorMessage.apply(firstUserId));
		}
		int secondUserConnectionCount = secondUserEntity.getConnectionCount();
		if (secondUserConnectionCount >= limitConnections) {
			throw new IllegalStateException(getErrorMessage.apply(secondUserId));
		}

		firstUserEntity.setConnectionCount(firstUserConnectionCount + 1);
		secondUserEntity.setConnectionCount(secondUserConnectionCount + 1);

		userConnectionEntity.setStatus(UserConnectionStatus.ACCEPTED);
	}
}
