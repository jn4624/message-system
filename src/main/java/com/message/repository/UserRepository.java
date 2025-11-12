package com.message.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.dto.domain.InviteCode;
import com.message.dto.projection.InviteCodeProjection;
import com.message.dto.projection.UsernameProjection;
import com.message.entity.UserEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByUsername(@NonNull String username);

	Optional<UsernameProjection> findByUserId(@NonNull Long userId);

	Optional<UserEntity> findByConnectionInviteCode(@NonNull String connectionInviteCode);

	Optional<InviteCodeProjection> findInviteCodeByUserId(@NonNull Long userId);

	/*
	  - 비관적 락 사용
	  - 하이버네이트가 쿼리 메서드를 가지고 쿼리를 만들 때
	    select 쿼리를 for update 구문을 붙여서 생성한다.
	  - 그럼 한 row에 락이 잡힌다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<UserEntity> findForUpdateByUserId(@NonNull Long userId);
}
