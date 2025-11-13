package com.message.dto.domain;

import com.message.constant.UserConnectionStatus;

public record Connection(String username, UserConnectionStatus status) {
}
