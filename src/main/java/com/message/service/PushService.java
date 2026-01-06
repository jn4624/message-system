package com.message.service;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.message.dto.domain.UserId;

@Service
public class PushService {

	private static final Logger log = LoggerFactory.getLogger(PushService.class);

	private final HashSet<String> pushMessageTypes = new HashSet<>();

	public void registerPushMessageType(String messageType) {
		pushMessageTypes.add(messageType);
	}

	public void pushMessage(UserId userId, String messageType, String message) {
		if (pushMessageTypes.contains(messageType)) {
			log.info("Push message: {} to user: {}", message, userId);
		}
	}
}
