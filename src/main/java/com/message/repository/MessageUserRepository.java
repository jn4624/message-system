package com.message.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import com.message.entity.MessageUserEntity;

@Repository
public interface MessageUserRepository extends JpaRepository<MessageUserEntity, Long> {

	Optional<MessageUserEntity> findByUsername(@NonNull String username);
}
