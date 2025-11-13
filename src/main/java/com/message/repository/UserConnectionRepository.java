package com.message.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.constant.UserConnectionStatus;
import com.message.dto.projection.InviterUserIdProjection;
import com.message.dto.projection.UserConnectionStatusProjection;
import com.message.dto.projection.UserIdUsernameProjection;
import com.message.entity.UserConnectionEntity;
import com.message.entity.UserConnectionId;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnectionEntity, UserConnectionId> {

	Optional<UserConnectionStatusProjection> findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(
		@NonNull Long partnerAUserId, @NonNull Long partnerBUserId);

	Optional<UserConnectionEntity> findByPartnerAUserIdAndPartnerBUserIdAndStatus(
		@NonNull Long partnerAUserId, @NonNull Long partnerBUserId, @NonNull UserConnectionStatus status);

	Optional<InviterUserIdProjection> findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
		@NonNull Long partnerAUserId, @NonNull Long partnerBUserId);

	@Query(
		"SELECT u.partnerBUserId AS userId, userB.username AS username "
			+ "FROM UserConnectionEntity u "
			+ "INNER JOIN UserEntity userB ON u.partnerBUserId = userB.userId "
			+ "WHERE u.partnerAUserId = :userId AND u.status = :status")
	List<UserIdUsernameProjection> findByPartnerAUserIdAndStatus(@NonNull @Param("userId") Long userId,
		@NonNull @Param("status") UserConnectionStatus status);

	@Query(
		"SELECT u.partnerAUserId AS userId, userA.username AS username "
			+ "FROM UserConnectionEntity u "
			+ "INNER JOIN UserEntity userA ON u.partnerAUserId = userA.userId "
			+ "WHERE u.partnerBUserId = :userId AND u.status = :status")
	List<UserIdUsernameProjection> findByPartnerBUserIdAndStatus(@NonNull @Param("userId") Long userId,
		@NonNull @Param("status") UserConnectionStatus status);
}
