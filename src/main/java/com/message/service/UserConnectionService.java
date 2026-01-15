package com.message.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.message.constant.KeyPrefix;
import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.InviteCode;
import com.message.dto.domain.User;
import com.message.dto.domain.UserId;
import com.message.dto.projection.UserIdUsernameInviterUserIdProjection;
import com.message.entity.UserConnectionEntity;
import com.message.repository.UserConnectionRepository;
import com.message.util.JsonUtil;

@Service
public class UserConnectionService {

	private static final Logger log = LoggerFactory.getLogger(UserConnectionService.class);

	private final UserService userService;
	private final UserConnectionLimitService userConnectionLimitService;
	private final CacheService cacheService;
	private final UserConnectionRepository userConnectionRepository;
	private final JsonUtil jsonUtil;
	private final long TTL = 600;

	public UserConnectionService(
		UserService userService,
		UserConnectionLimitService userConnectionLimitService,
		CacheService cacheService,
		UserConnectionRepository userConnectionRepository,
		JsonUtil jsonUtil
	) {
		this.userService = userService;
		this.userConnectionLimitService = userConnectionLimitService;
		this.cacheService = cacheService;
		this.userConnectionRepository = userConnectionRepository;
		this.jsonUtil = jsonUtil;
	}

	/*
	  - 2개의 조회 쿼리가 존재할 경우 DB 커넥션풀이 쓰고 반납하고가 반복된다
	  - @Transactional(readOnly = true)을 명시적으로 붙여주면
	    하나의 커넥션 풀에서 2개의 조회 쿼리가 실행되어 성능상으로는 좀 더 이점이 있다
	 */
	@Transactional(readOnly = true)
	public List<User> getUsersByStatus(UserId userId, UserConnectionStatus status) {
		String key = cacheService.buildKey(KeyPrefix.CONNECTIONS_STATUS, userId.id().toString(), status.name());
		Optional<String> cachedUsers = cacheService.get(key);

		if (cachedUsers.isPresent()) {
			return jsonUtil.fromJsonToList(cachedUsers.get(), User.class);
		}

		List<UserIdUsernameInviterUserIdProjection> usersA =
			userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), status);
		List<UserIdUsernameInviterUserIdProjection> usersB =
			userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), status);

		List<User> findUsers;
		if (status == UserConnectionStatus.ACCEPTED) {
			findUsers = Stream.concat(usersA.stream(), usersB.stream())
				.map(item ->
					new User(new UserId(item.getUserId()), item.getUsername())).toList();
		} else {
			findUsers = Stream.concat(usersA.stream(), usersB.stream())
				.filter(item -> !item.getInviterUserId().equals(userId.id()))
				.map(item ->
					new User(new UserId(item.getUserId()), item.getUsername())).toList();
		}

		if (!findUsers.isEmpty()) {
			jsonUtil.toJson(findUsers)
				.ifPresent(json -> cacheService.set(key, json, TTL));
		}
		return findUsers;
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
					.filter(count ->
						count >= userConnectionLimitService.getLimitConnections()).isPresent()) {
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
					TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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

	/*
	  동시성이 깨지는 이슈 발생
	  - 원인:
	    - 기존에는 accept 메서드에 트랜잭션 어노테이션을 선언하지 않아
	      메서드 내부에서 데이터베이스에 접근할 때 각각 별도의 커넥션을 사용했다
	    - 하지만 readonly 처리를 적용하면서 트랜잭션 어노테이션이 선언되어
	      accept 메서드 내부에서 데이터베이스에 접근할 때 하나의 커넥션을 함께 사용하게 되었다
	    - 이때 accept 비즈니스 로직 중 UserEntity를 조회하는 쿼리가 여러개이고
	      락 + UserEntity 조회 쿼리보다 UserEntity 조회 쿼리가 우선 실행된다면
	      JPA는 락 + UserEntity 조회를 실행할 때 이전에 조회한 데이터를 영속성 컨텍스트에서 꺼내 반환한다
	    - 따라서 락 + 조회 쿼리는 실행되지 않고 락이 걸리지 않게 된다
	  - 해결방법:
	    1. UserEntity 조회 쿼리를 중복 사용하지 말고 명시적으로 필요한 쿼리를 생성하여 사용한다
	    2. 내부 트랜잭션 전파를(락 + 조회) Propagation.REQUIRES_NEW로 변경한다
	 */
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

		if (getInviterUserId(accepterUserId, inviterUserId)
			.filter(invitationSenderUserId ->
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
			/*
			   - 내부 트랜잭션에서 발생한 IllegalStateException 예외가 외부로 전파되었지만
			     외부 트랜잭션에서는 해당 예외를 catch로 처리하였기 때문에 외부 트랜잭션은 커밋을 시도한다
			   - 하지만 트랜잭션은 각 트랜잭션의 상태를 저장해놓기 때문에
			     외부 트랜잭션이 커밋하는 시점에 내부 트랜잭션이 롤백되었다는 사실을 알고 아래와 같은 예외를 발생시킨다
			     Exception in thread "Thread-11" org.springframework.transaction.UnexpectedRollbackException:
			     Transaction silently rolled back because it has been marked as rollback-only 예외가 발생한다
			   - 따라서 예외 없이 외부 트랜잭션도 정상 처리되게 하려면 의도적으로 롤백 처리를 해줘야 한다
			   - 의도적인 롤백 처리시 문제 발생:
			     - 스프링부트를 사용하지 않는 단위 테스트의 경우 트랜잭션의 상태가 존재하지 않아 예외가 발생한다
			     - 이를 해결하기 위해서는 트랜잭션이 활성화되었는지 확인 먼저 하는 방어 코드를 추가한다
			 */
			if (TransactionSynchronizationManager.isActualTransactionActive()) { // 트랜잭션 활성화 여부 체크
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // 의도적인 롤백 처리
			}
			return Pair.of(Optional.empty(), e.getMessage());
		} catch (Exception e) {
			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			}
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
					TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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
					if (TransactionSynchronizationManager.isActualTransactionActive()) {
						TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
					}
					log.error("Disconnect failed. cause: {}", e.getMessage());
				}

				return Pair.of(false, "Disconnect failed");
			}).orElse(Pair.of(false, "Disconnect failed"));
	}

	@Transactional(readOnly = true)
	private Optional<UserId> getInviterUserId(UserId parterAUserId, UserId parterBUserId) {
		long partnerA = Long.min(parterAUserId.id(), parterBUserId.id());
		long partnerB = Long.max(parterAUserId.id(), parterBUserId.id());

		String key = cacheService.buildKey(
			KeyPrefix.INVITER_USER_ID, String.valueOf(partnerA), String.valueOf(partnerB));
		Optional<String> cachedInviterUserId = cacheService.get(key);

		if (cachedInviterUserId.isPresent()) {
			return Optional.of(new UserId(Long.parseLong(cachedInviterUserId.get())));
		}

		Optional<UserId> findInviterUserId =
			userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(partnerA, partnerB)
				.map(inviterUserIdProjection ->
					new UserId(inviterUserIdProjection.getInviterUserId()));
		findInviterUserId.ifPresent(userId -> cacheService.set(key, userId.id().toString(), TTL));
		return findInviterUserId;
	}

	@Transactional(readOnly = true)
	private UserConnectionStatus getStatus(UserId inviterUserId, UserId partnerUserId) {
		long partnerA = Long.min(inviterUserId.id(), partnerUserId.id());
		long partnerB = Long.max(inviterUserId.id(), partnerUserId.id());

		String key = cacheService.buildKey(
			KeyPrefix.CONNECTION_STATUS, String.valueOf(partnerA), String.valueOf(partnerB));
		Optional<String> cachedUserConnectionStatus = cacheService.get(key);

		if (cachedUserConnectionStatus.isPresent()) {
			return UserConnectionStatus.valueOf(cachedUserConnectionStatus.get());
		}

		UserConnectionStatus findConnectionStatus =
			userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(partnerA, partnerB)
				.map(status ->
					UserConnectionStatus.valueOf(status.getStatus()))
				.orElse(UserConnectionStatus.NONE);
		cacheService.set(key, findConnectionStatus.name(), TTL);
		return findConnectionStatus;
	}

	@Transactional
	private void setStatus(UserId inviterUserId, UserId partnerUserId, UserConnectionStatus userConnectionStatus) {
		if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
			throw new IllegalArgumentException("Can't set to accepted");
		}

		long partnerA = Long.min(inviterUserId.id(), partnerUserId.id());
		long partnerB = Long.max(inviterUserId.id(), partnerUserId.id());

		userConnectionRepository.save(
			new UserConnectionEntity(partnerA, partnerB, userConnectionStatus, inviterUserId.id()));

		cacheService.delete(List.of(
			cacheService.buildKey(
				KeyPrefix.CONNECTION_STATUS, String.valueOf(partnerA), String.valueOf(partnerB)),
			cacheService.buildKey(
				KeyPrefix.CONNECTIONS_STATUS, inviterUserId.id().toString(), userConnectionStatus.name()),
			cacheService.buildKey(
				KeyPrefix.CONNECTIONS_STATUS, partnerUserId.id().toString(), userConnectionStatus.name())));
	}
}
