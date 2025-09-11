package com.message.entity;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "message")
public class MessageEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "message_sequence", nullable = false)
	private Long messageSequence;

	@Column(name = "user_name", nullable = false)
	private String username;

	@Column(name = "content", nullable = false)
	private String content;

	public MessageEntity() {
	}

	public MessageEntity(String username, String content) {
		this.username = username;
		this.content = content;
	}

	public Long getMessageSequence() {
		return messageSequence;
	}

	public String getUsername() {
		return username;
	}

	public String getContent() {
		return content;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageEntity that = (MessageEntity)o;
		return Objects.equals(getMessageSequence(), that.getMessageSequence());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getMessageSequence());
	}

	@Override
	public String toString() {
		return "MessageEntity{messageSequence=%d, username='%s', content='%s', createdAt=%s, updatedAt=%s}"
			.formatted(messageSequence, username, content, getCreatedAt(), getUpdatedAt());
	}
}
