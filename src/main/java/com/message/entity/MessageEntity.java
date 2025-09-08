package com.message.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "message")
public class MessageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "message_sequence", nullable = false)
	private Long messageSequence;

	@Column(name = "user_name", nullable = false)
	private String username;

	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
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
			.formatted(messageSequence, username, content, createdAt, updatedAt);
	}
}
