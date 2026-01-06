package com.message.handler.websocket;

import java.util.Optional;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.ResultType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.EnterRequest;
import com.message.dto.websocket.outbound.EnterResponse;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.service.ChannelService;
import com.message.service.ClientNotificationService;

@Component
public class EnterRequestHandler implements BaseRequestHandler<EnterRequest> {

	private final ChannelService channelService;
	private final ClientNotificationService clientNotificationService;

	public EnterRequestHandler(
		ChannelService channelService,
		ClientNotificationService clientNotificationService
	) {
		this.channelService = channelService;
		this.clientNotificationService = clientNotificationService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, EnterRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		Pair<Optional<String>, ResultType> result = channelService.enter(request.getChannelId(), senderUserId);
		result.getFirst().ifPresentOrElse(title ->
				clientNotificationService.sendMessage(
					senderSession, senderUserId, new EnterResponse(request.getChannelId(), title))
			, () -> clientNotificationService.sendMessage(
				senderSession, senderUserId, new ErrorResponse(
					MessageType.ENTER_REQUEST, result.getSecond().getMessage())));
	}
}
