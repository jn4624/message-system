package com.message.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.InviteCode;
import com.message.dto.domain.User;
import com.message.dto.domain.UserId;
import com.message.dto.projection.UserIdUsernameInviterUserIdProjection;
import com.message.entity.UserConnectionEntity;
import com.message.repository.UserConnectionRepository;

@Service
public class UserConnectionService {

	private static final Logger log = LoggerFactory.getLogger(UserConnectionService.class);

	private final UserService userService;
	private final UserConnectionLimitService userConnectionLimitService;
	private final UserConnectionRepository userConnectionRepository;

	public UserConnectionService(
		UserService userService,
		UserConnectionLimitService userConnectionLimitService,
		UserConnectionRepository userConnectionRepository
	) {
		this.userService = userService;
		this.userConnectionLimitService = userConnectionLimitService;
		this.userConnectionRepository = userConnectionRepository;
	}

	/*
	  - 2개의 조회 쿼리가 존재할 경우 DB 커넥션풀이 쓰고 반납하고가 반복된다
	  - @Transactional(readOnly = true)을 명시적으로 붙여주면
	    하나의 커넥션 풀에서 2개의 조회 쿼리가 실행되어 성능상으로는 좀 더 이점이 있다
	 */
	@Transactional(readOnly = true)
	public List<User> getUsersByStatus(UserId userId, UserConnectionStatus status) {
		List<UserIdUsernameInviterUserIdProjection> usersA =
			userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), status);
		List<UserIdUsernameInviterUserIdProjection> usersB =
			userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), status);

		if (status == UserConnectionStatus.ACCEPTED) {
			return Stream.concat(usersA.stream(), usersB.stream())
				.map(item ->
					new User(new UserId(item.getUserId()), item.getUsername())).toList();
		} else {
			return Stream.concat(usersA.stream(), usersB.stream())
				.filter(item -> !item.getInviterUserId().equals(userId.id()))
				.map(item ->
					new User(new UserId(item.getUserId()), item.getUsername())).toList();
		}
	}

	@Transactional(readOnly = true)
	public long countConnectionStatus(UserId senderUserId, List<UserId> partnerUserIds, UserConnectionStatus status) {
		List<Long> ids = partnerUserIds.stream().map(UserId::id).toList();
		return userConnectionRepository.countByPartnerAUserIdAndPartnerBUserIdInAndStatus(
			senderUserId.id(), ids, status)
			+ userConnectionRepository.countByPartnerBUserIdAndPartnerAUserIdInAndStatus(
			senderUserId.id(), ids, status);
	}

	@Transactional
	public Pair<Optional<UserId>, String> invite(UserId inviterUserId, InviteCode inviteCode) {
		Optional<User> partner = userService.getUser(inviteCode); // 연결할 대상
		if (partner.isEmpty()) {
			log.info("Invalid invite code. {}, from{}", inviteCode, inviterUserId);
			return Pair.of(Optional.empty(), "Invalid invite code");
		}

		UserId partnerUserId = partner.get().userId();
		String partnerUsername = partner.get().username();
		if (partnerUserId.equals(inviterUserId)) {
			return Pair.of(Optional.empty(), "Can't self invite");
		}

		UserConnectionStatus userConnectionStatus = getStatus(inviterUserId, partnerUserId);
		return switch (userConnectionStatus) {
			case NONE, DISCONNECTED -> {
				if (userService.getConnectionCount(inviterUserId)
					.filter(count -> count >= userConnectionLimitService.getLimitConnections()).isPresent()) {
					yield Pair.of(Optional.empty(), "Connection limit reached");
				}

				Optional<String> inviterUsername = userService.getUsername(inviterUserId);
				if (inviterUsername.isEmpty()) {
					log.warn("InviteRequest failed");
					yield Pair.of(Optional.empty(), "InviteRequest failed");
				}

				try {
					setStatus(inviterUserId, partnerUserId, UserConnectionStatus.PENDING);
					yield Pair.of(Optional.of(partnerUserId), inviterUsername.get());
				} catch (Exception e) {
					log.error("Set pending failed. cause: {}", e.getMessage());
					yield Pair.of(Optional.empty(), "InviteRequest failed");
				}
			}
			case ACCEPTED -> Pair.of(Optional.empty(), "Already connected with " + partnerUsername);
			case PENDING, REJECTED -> {
				log.info("{} invites {} but does not deliver the invitation request", inviterUserId, partnerUsername);
				yield Pair.of(Optional.empty(), "Already invited to " + partnerUsername);
			}
		};
	}

	@Transactional
	public Pair<Optional<UserId>, String> accept(UserId accepterUserId, String inviterUsername) {
		Optional<UserId> userId = userService.getUserId(inviterUsername);
		if (userId.isEmpty()) {
			return Pair.of(Optional.empty(), "Invalid username");
		}

		UserId inviterUserId = userId.get();
		if (accepterUserId.equals(inviterUserId)) {
			return Pair.of(Optional.empty(), "Can't self accept");
		}

		if (getInviterUserId(accepterUserId, inviterUserId).filter(invitationSenderUserId ->
			invitationSenderUserId.equals(inviterUserId)).isEmpty()) {
			return Pair.of(Optional.empty(), "Invalid username");
		}

		UserConnectionStatus userConnectionStatus = getStatus(inviterUserId, accepterUserId);
		if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
			return Pair.of(Optional.empty(), "Already connected");
		}
		if (userConnectionStatus != UserConnectionStatus.PENDING) {
			return Pair.of(Optional.empty(), "Accept failed");
		}

		Optional<String> acceptUsername = userService.getUsername(accepterUserId);
		if (acceptUsername.isEmpty()) {
			log.error("Invalid userId. userId: {}", accepterUserId);
			return Pair.of(Optional.empty(), "Accept failed");
		}

		try {
			userConnectionLimitService.accept(accepterUserId, inviterUserId);
			return Pair.of(Optional.of(inviterUserId), acceptUsername.get());
		} catch (IllegalStateException e) {
			return Pair.of(Optional.empty(), e.getMessage());
		} catch (Exception e) {
			log.error("Accept failed. cause: {}", e.getMessage());
			return Pair.of(Optional.empty(), "Accept failed");
		}
	}

	@Transactional
	public Pair<Boolean, String> reject(UserId senderUserId, String inviterUsername) {
		return userService.getUserId(inviterUsername)
			.filter(inviterUserId -> !inviterUserId.equals(senderUserId))
			.filter(inviterUserId -> getInviterUserId(inviterUserId, senderUserId)
				.filter(invitationSenderUserId -> invitationSenderUserId.equals(inviterUserId)).isPresent())
			.filter(inviterUserId -> getStatus(inviterUserId, senderUserId) == UserConnectionStatus.PENDING)
			.map(inviterUserId -> {
				try {
					setStatus(inviterUserId, senderUserId, UserConnectionStatus.REJECTED);
					return Pair.of(true, inviterUsername);
				} catch (Exception e) {
					log.error("Set rejected failed. cause: {}", e.getMessage());
					return Pair.of(false, "Reject failed");
				}
			}).orElse(Pair.of(false, "Reject failed"));
	}

	@Transactional
	public Pair<Boolean, String> disconnect(UserId senderUserId, String partnerUsername) {
		return userService.getUserId(partnerUsername)
			.filter(partnerUserId -> !senderUserId.equals(partnerUserId))
			.map(partnerUserId -> {
				try {
					UserConnectionStatus userConnectionStatus = getStatus(senderUserId, partnerUserId);
					if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
						userConnectionLimitService.disconnect(senderUserId, partnerUserId);
						return Pair.of(true, partnerUsername);
					} else if (userConnectionStatus == UserConnectionStatus.REJECTED
						&& getInviterUserId(senderUserId, partnerUserId)
						.filter(inviterUserId -> inviterUserId.equals(partnerUserId)).isPresent()) {
						setStatus(senderUserId, partnerUserId, UserConnectionStatus.DISCONNECTED);
						return Pair.of(true, partnerUsername);
					}
				} catch (Exception e) {
					log.error("Disconnect failed. cause: {}", e.getMessage());
				}

				return Pair.of(false, "Disconnect failed");
			}).orElse(Pair.of(false, "Disconnect failed"));
	}

	@Transactional(readOnly = true)
	private Optional<UserId> getInviterUserId(UserId parterAUserId, UserId parterBUserId) {
		return userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
				Long.min(parterAUserId.id(), parterBUserId.id()),
				Long.max(parterAUserId.id(), parterBUserId.id()))
			.map(inviterUserIdProjection -> new UserId(inviterUserIdProjection.getInviterUserId()));
	}

	@Transactional(readOnly = true)
	private UserConnectionStatus getStatus(UserId inviterUserId, UserId partnerUserId) {
		return userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(
				Long.min(inviterUserId.id(), partnerUserId.id()),
				Long.max(inviterUserId.id(), partnerUserId.id()))
			.map(status -> UserConnectionStatus.valueOf(status.getStatus()))
			.orElse(UserConnectionStatus.NONE);
	}

	@Transactional
	private void setStatus(UserId inviterUserId, UserId partnerUserId, UserConnectionStatus userConnectionStatus) {
		if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
			throw new IllegalArgumentException("Can't set to accepted");
		}

		userConnectionRepository.save(new UserConnectionEntity(
			Long.min(inviterUserId.id(), partnerUserId.id()),
			Long.max(inviterUserId.id(), partnerUserId.id()),
			userConnectionStatus,
			inviterUserId.id()));
	}
}
