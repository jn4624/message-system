package com.message.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_channel")
@IdClass(UserChannelId.class)
public class UserChannelEntity extends BaseEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Id
	@Column(name = "channel_id", nullable = false)
	private Long channelId;

	@Column(name = "last_read_msg_seq", nullable = false)
	private long lastReadMsgSeq;

	public UserChannelEntity() {
	}

	public UserChannelEntity(Long userId, Long channelId, long lastReadMsgSeq) {
		this.userId = userId;
		this.channelId = channelId;
		this.lastReadMsgSeq = lastReadMsgSeq;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getChannelId() {
		return channelId;
	}

	public long getLastReadMsgSeq() {
		return lastReadMsgSeq;
	}

	public void setLastReadMsgSeq(long lastReadMsgSeq) {
		this.lastReadMsgSeq = lastReadMsgSeq;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		UserChannelEntity that = (UserChannelEntity)o;
		return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getChannelId(),
			that.getChannelId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUserId(), getChannelId());
	}

	@Override
	public String toString() {
		return "UserChannelEntity{userId=%d, channelId=%d, lastReadMsgSeq=%d}"
			.formatted(userId, channelId, lastReadMsgSeq);
	}
}
