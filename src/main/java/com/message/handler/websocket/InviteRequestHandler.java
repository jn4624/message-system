package com.message.handler.websocket;

import java.util.Optional;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.constant.UserConnectionStatus;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.InviteRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.InviteNotification;
import com.message.dto.websocket.outbound.InviteResponse;
import com.message.service.ClientNotificationService;
import com.message.service.UserConnectionService;

@Component
public class InviteRequestHandler implements BaseRequestHandler<InviteRequest> {

	private final UserConnectionService userConnectionService;
	private final ClientNotificationService clientNotificationService;

	public InviteRequestHandler(
		UserConnectionService userConnectionService,
		ClientNotificationService clientNotificationService
	) {
		this.userConnectionService = userConnectionService;
		this.clientNotificationService = clientNotificationService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, InviteRequest request) {
		UserId inviterUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());
		Pair<Optional<UserId>, String> result =
			userConnectionService.invite(inviterUserId, request.getUserInviteCode());

		result.getFirst().ifPresentOrElse(partnerUserId -> {
			String inviterUsername = result.getSecond();

			clientNotificationService.sendMessage(
				senderSession, inviterUserId, new InviteResponse(
					request.getUserInviteCode(), UserConnectionStatus.PENDING));

			clientNotificationService.sendMessage(partnerUserId, new InviteNotification(inviterUsername));
		}, () -> {
			String errorMessage = result.getSecond();
			clientNotificationService.sendMessage(
				senderSession, inviterUserId, new ErrorResponse(MessageType.INVITE_REQUEST, errorMessage));
		});
	}
}
