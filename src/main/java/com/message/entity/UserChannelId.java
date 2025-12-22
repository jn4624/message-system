package com.message.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserChannelId implements Serializable {

	private Long userId;
	private Long channelId;

	public UserChannelId() {
	}

	public UserChannelId(Long userId, Long channelId) {
		this.userId = userId;
		this.channelId = channelId;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getChannelId() {
		return channelId;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		UserChannelId that = (UserChannelId)o;
		return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(
			getChannelId(), that.getChannelId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUserId(), getChannelId());
	}

	@Override
	public String toString() {
		return "UserChannelId{userId=%d, channelId=%d}"
			.formatted(userId, channelId);
	}
}
