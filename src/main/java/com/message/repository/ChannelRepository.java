package com.message.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.dto.projection.ChannelTitleProjection;
import com.message.entity.ChannelEntity;

@Repository
public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {

	Optional<ChannelTitleProjection> findChannelTitleByChannelId(@NonNull Long channelId);
}
