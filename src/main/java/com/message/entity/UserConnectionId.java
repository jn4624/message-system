package com.message.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserConnectionId implements Serializable {

	private Long partnerAUserId;
	private Long partnerBUserId;

	public UserConnectionId() {
	}

	public UserConnectionId(Long partnerAUserId, Long partnerBUserId) {
		this.partnerAUserId = partnerAUserId;
		this.partnerBUserId = partnerBUserId;
	}

	public Long getPartnerAUserId() {
		return partnerAUserId;
	}

	public Long getPartnerBUserId() {
		return partnerBUserId;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		UserConnectionId that = (UserConnectionId)o;
		return Objects.equals(getPartnerAUserId(), that.getPartnerAUserId()) && Objects.equals(
			getPartnerBUserId(), that.getPartnerBUserId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getPartnerAUserId(), getPartnerBUserId());
	}

	@Override
	public String toString() {
		return "UserConnectionId{partnerAUserId=%d, partnerBUserId=%d}"
			.formatted(partnerAUserId, partnerBUserId);
	}
}
