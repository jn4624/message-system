package com.message.util;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonUtil {

	private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

	private final ObjectMapper objectMapper;

	public JsonUtil(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public <T> Optional<T> fromJson(String json, Class<T> clazz) {
		try {
			return Optional.of(objectMapper.readValue(json, clazz));
		} catch (Exception e) {
			log.error("Failed JSON to Object: {}", e.getMessage());
			return Optional.empty();
		}
	}

	public Optional<String> toJson(Object object) {
		try {
			return Optional.of(objectMapper.writeValueAsString(object));
		} catch (Exception e) {
			log.error("Failed Object to JSON: {}", e.getMessage());
			return Optional.empty();
		}
	}
}
