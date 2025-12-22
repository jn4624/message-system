package com.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.entity.UserChannelId;
import com.message.entity.UserChannelEntity;

@Repository
public interface UserChannelRepository extends JpaRepository<UserChannelEntity, UserChannelId> {

	boolean existsByUserIdAndChannelId(@NonNull Long userId, @NonNull Long channelId);
}
