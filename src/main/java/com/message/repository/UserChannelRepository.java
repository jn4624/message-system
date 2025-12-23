package com.message.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.dto.projection.UserIdProjection;
import com.message.entity.UserChannelId;
import com.message.entity.UserChannelEntity;

@Repository
public interface UserChannelRepository extends JpaRepository<UserChannelEntity, UserChannelId> {

	boolean existsByUserIdAndChannelId(@NonNull Long userId, @NonNull Long channelId);

	List<UserIdProjection> findUserIdsByChannelId(@NonNull Long channelId);
}
