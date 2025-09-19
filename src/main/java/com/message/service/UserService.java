package com.message.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.message.dto.domain.InviteCode;
import com.message.dto.domain.User;
import com.message.dto.domain.UserId;
import com.message.dto.projection.UsernameProjection;
import com.message.entity.UserEntity;
import com.message.repository.UserRepository;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);
	private final SessionService sessionService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(
		SessionService sessionService,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder
	) {
		this.sessionService = sessionService;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public Optional<String> getUsername(UserId userId) {
		return userRepository.findByUserId(userId.id()).map(UsernameProjection::getUsername);
	}

	public Optional<User> getUser(InviteCode inviteCode) {
		return userRepository.findByConnectionInviteCode(inviteCode.code())
			.map(entity -> new User(new UserId(entity.getUserId()), entity.getUsername()));
	}

	@Transactional
	public UserId addUser(String username, String password) {
		UserEntity userEntity = userRepository.save(
			new UserEntity(username, passwordEncoder.encode(password)));

		log.info("User registered. userId: {}, username: {}", userEntity.getUserId(),
			userEntity.getUsername());

		return new UserId(userEntity.getUserId());
	}

	@Transactional
	public void removeUser() {
		String username = sessionService.getUsername();

		UserEntity userEntity = userRepository.findByUsername(username).orElseThrow();
		userRepository.deleteById(userEntity.getUserId());

		log.info("User unregistered. userId: {}, username: {}",
			userEntity.getUserId(), userEntity.getUsername());
	}
}
