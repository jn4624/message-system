package com.message.handler.websocket;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.dto.websocket.inbound.BaseRequest;

import jakarta.annotation.PostConstruct;

@Component
@SuppressWarnings({"rawtypes", "unchecked"})
public class RequestDispatcher {

	private static final Logger log = LoggerFactory.getLogger(RequestDispatcher.class);

	/*
	  - 멀티스레드에서 다양하게 접근핥 예정이지만, ConcurrentHashMap을 사용하지 않고 HashMap을 사용하는 이유
	  - 이유: handlerMap은 애플리케이션이 시작될 때 단 한번만 초기화되고, 그 이후에는 읽기 전용으로 사용되기 때문
	  - 즉, 멀티스레드 환경에서 동시 접근이 발생하더라도, 읽기 작업만 수행되므로 HashMap을 사용해도 안전하다.
	  - 만약 handlerMap이 자주 변경되거나, 쓰기 작업이 빈번하게 발생하는 경우에는 ConcurrentHashMap을 사용하는 것이 좋다.
	 */
	private final Map<Class<? extends BaseRequest>, BaseRequestHandler<? extends BaseRequest>> handlerMap = new HashMap<>();
	private final ListableBeanFactory listableBeanFactory;

	public RequestDispatcher(ListableBeanFactory listableBeanFactory) {
		this.listableBeanFactory = listableBeanFactory;
	}

	public <T extends BaseRequest> void dispatchRequest(WebSocketSession webSocketSession, T request) {
		BaseRequestHandler<T> handler = (BaseRequestHandler<T>)handlerMap.get(request.getClass());

		if (handler != null) {
			handler.handleRequest(webSocketSession, request);
			return;
		}

		log.error("Handler not found for request type: {}", request.getClass().getSimpleName());
	}

	@PostConstruct
	private void prepareRequestHandlerMapping() {
		Map<String, BaseRequestHandler> beanHandler = listableBeanFactory.getBeansOfType(BaseRequestHandler.class);

		for (BaseRequestHandler handler : beanHandler.values()) {
			Class<? extends BaseRequest> requestClass = extractRequestClass(handler);

			if (requestClass != null) {
				handlerMap.put(requestClass, handler);
			}
		}
	}

	private Class<? extends BaseRequest> extractRequestClass(BaseRequestHandler handler) {
		for (Type type : handler.getClass().getGenericInterfaces()) {
			if (type instanceof ParameterizedType parameterizedType &&
				parameterizedType.getRawType().equals(BaseRequestHandler.class)
			) {
				return (Class<? extends BaseRequest>)parameterizedType.getActualTypeArguments()[0];
			}
		}
		return null;
	}
}
