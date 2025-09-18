package com.message.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.message.entity.UserEntity;
import com.message.repository.UserRepository;

@Service
public class MessageUserDetailsService implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(MessageUserDetailsService.class);
	private final UserRepository userRepository;

	public MessageUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// Filter 내부에서 자동으로 사용될 예정
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserEntity userEntity = userRepository.findByUsername(username)
			.orElseThrow(() -> {
				log.info("User not found: {}", username);
				return new UsernameNotFoundException("");
			});

		return new MessageUserDetails(
			userEntity.getUserId(), userEntity.getUsername(), userEntity.getPassword());
	}
}
