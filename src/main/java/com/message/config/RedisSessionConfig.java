package com.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableRedisHttpSession(redisNamespace = "message:user_session", maxInactiveIntervalInSeconds = 300) // TTL: 5분
public class RedisSessionConfig {

	/*
	  - Redis에 저장되는 session 정보들의 직렬화를 위한 설정
	  - Redis에 저장된 데이터를 확인해보면 역직렬화할 때 각 정보가 어떤 클래스로 생성되어야 한다는걸
	    알고 있어야 하기 때문에 관련 메타 정보가 남는다.
	  - MessageUserDetails 클래스에 관련 어노테이션 설정 필요
	 */
	@Bean
	public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
		/*
		  - 메서드 내부에서 따로 생성해서 사용하는 이유는 설정을 변경하기 위함이다.
		  - 주입 받아서 설정을 변경하게 되면 전역 ObjectMapper의 설정이 변경되기 때문에.
		 */
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
		return new GenericJackson2JsonRedisSerializer(objectMapper);
	}
}
