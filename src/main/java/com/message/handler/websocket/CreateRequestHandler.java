package com.message.handler.websocket;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.ResultType;
import com.message.dto.domain.Channel;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.CreateRequest;
import com.message.dto.websocket.outbound.CreateResponse;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.JoinNotification;
import com.message.service.ChannelService;
import com.message.service.ClientNotificationService;
import com.message.service.UserService;

@Component
public class CreateRequestHandler implements BaseRequestHandler<CreateRequest> {

	private final ChannelService channelService;
	private final UserService userService;
	private final ClientNotificationService clientNotificationService;

	public CreateRequestHandler(
		ChannelService channelService,
		UserService userService,
		ClientNotificationService clientNotificationService
	) {
		this.channelService = channelService;
		this.userService = userService;
		this.clientNotificationService = clientNotificationService;
	}

	/*
	   - 프로덕션 환경이라면, 입력된 값에 대한 체크를 진행하여 파라미터를 검증 처리가 필요하다
	   - 클라이언트가 올려주는 값을 100% 신뢰하고 사용할 수 없기 때문에
	     검증을 더 세세하게 진행해야 하는데 생략되었음을 참고해야 한다
	 */
	@Override
	public void handleRequest(WebSocketSession senderSession, CreateRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		List<UserId> participantUserIds = userService.getUserIds(request.getParticipantUsernames());
		if (participantUserIds.isEmpty()) {
			clientNotificationService.sendMessage(
				senderSession, senderUserId, new ErrorResponse(
					MessageType.CREATE_REQUEST, ResultType.NOT_FOUND.getMessage()));
			return;
		}

		Pair<Optional<Channel>, ResultType> result;

		try {
			result = channelService.create(senderUserId, participantUserIds, request.getTitle());
		} catch (Exception e) {
			clientNotificationService.sendMessage(
				senderSession, senderUserId, new ErrorResponse(
					MessageType.CREATE_REQUEST, ResultType.FAILED.getMessage()));
			return;
		}

		if (result.getFirst().isEmpty()) {
			clientNotificationService.sendMessage(
				senderSession, senderUserId, new ErrorResponse(
					MessageType.CREATE_REQUEST, result.getSecond().getMessage()));
			return;
		}

		Channel channel = result.getFirst().get();
		clientNotificationService.sendMessage(
			senderSession, senderUserId, new CreateResponse(channel.channelId(), channel.title()));

		participantUserIds.forEach(participantUserId ->
			CompletableFuture.runAsync(() ->
				clientNotificationService.sendMessage(
					participantUserId, new JoinNotification(channel.channelId(), channel.title()))));
	}
}
