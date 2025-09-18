package com.message.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.constant.UserConnectionStatus;
import com.message.dto.projection.InviterUserIdProjection;
import com.message.dto.projection.UserConnectionStatusProjection;
import com.message.entity.UserConnectionEntity;
import com.message.entity.UserConnectionId;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnectionEntity, UserConnectionId> {

	Optional<UserConnectionStatusProjection> findByPartnerAUserIdAndPartnerBUserId(
		@NonNull Long partnerAUserId, @NonNull Long partnerBUserId);

	Optional<UserConnectionEntity> findByPartnerAUserIdAndPartnerBUserIdAndStatus(
		@NonNull Long partnerAUserId, @NonNull Long partnerBUserId, @NonNull UserConnectionStatus status);

	Optional<InviterUserIdProjection> findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
		@NonNull Long partnerAUserId, @NonNull Long partnerBUserId);
}
