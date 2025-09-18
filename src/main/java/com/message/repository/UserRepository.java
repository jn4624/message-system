package com.message.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.dto.projection.UsernameProjection;
import com.message.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByUsername(@NonNull String username);

	Optional<UsernameProjection> findByUserId(@NonNull Long userId);

	Optional<UserEntity> findByConnectionInviteCode(@NonNull String connectionInviteCode);
}
