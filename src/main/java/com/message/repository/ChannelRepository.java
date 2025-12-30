package com.message.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.message.dto.projection.ChannelProjection;
import com.message.dto.projection.ChannelTitleProjection;
import com.message.dto.projection.InviteCodeProjection;
import com.message.entity.ChannelEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {

	Optional<ChannelTitleProjection> findChannelTitleByChannelId(@NonNull Long channelId);

	Optional<InviteCodeProjection> findChannelInviteCodeByChannelId(@NonNull Long channelId);

	Optional<ChannelProjection> findChannelByInviteCode(@NonNull String inviteCode);

	/*
	  아래와 같이 비관적 락을 잡는 방법과
		  @Transactional
		  @Modifying
		  @Query("update ChannelEntity c set c.headCount = ?1 where c.channelId = ?2")
		  int updateHeadCountByChannelId(int headCount, Long channelId);
	  위와 같이 업데이트 쿼리를 사용하는 방식이 존재한다
	  업데이트 쿼리를 사용하는 것도 비관적 락이 잡힌다

	  차이가 있다면
	  - 비관적 락을 잡은 후 업데이트를 처리하면 현재 headCount가 몇인지 확인 후 튕겨낼 수 있는데
	  - 바로 업데이트 쿼리를 사용하게 되면 업데이트가 실패했을 때
	    headCount가 초과되어 실패한건지, channelId를 찾지 못해 실패했는지 구분할 방법이 없다
	    왜냐하면 업데이트한 로우가 없다고 0이 리턴될 것이기 때문에
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ChannelEntity> findForUpdateByChannelId(@NonNull Long channelId);
}
