package com.message.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.message.entity.MessageUserEntity;
import com.message.repository.MessageUserRepository;

@Service
public class MessageUserDetailsService implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(MessageUserDetailsService.class);
	private final MessageUserRepository messageUserRepository;

	public MessageUserDetailsService(MessageUserRepository messageUserRepository) {
		this.messageUserRepository = messageUserRepository;
	}

	// Filter 내부에서 자동으로 사용될 예정
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		MessageUserEntity messageUserEntity = messageUserRepository.findByUsername(username)
			.orElseThrow(() -> {
				log.info("User not found: {}", username);
				return new UsernameNotFoundException("");
			});

		return new MessageUserDetails(
			messageUserEntity.getUserId(), messageUserEntity.getUsername(), messageUserEntity.getPassword());
	}
}
