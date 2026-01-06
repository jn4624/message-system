package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.FetchUserInviteCodeRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.FetchUserInviteCodeResponse;
import com.message.service.ClientNotificationService;
import com.message.service.UserService;

@Component
public class FetchUserInviteCodeRequestHandler implements BaseRequestHandler<FetchUserInviteCodeRequest> {

	private final UserService userService;
	private final ClientNotificationService clientNotificationService;

	public FetchUserInviteCodeRequestHandler(
		UserService userService,
		ClientNotificationService clientNotificationService
	) {
		this.userService = userService;
		this.clientNotificationService = clientNotificationService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, FetchUserInviteCodeRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());
		userService.getInviteCode(senderUserId)
			.ifPresentOrElse(inviteCode -> clientNotificationService.sendMessage(
					senderSession, senderUserId, new FetchUserInviteCodeResponse(inviteCode)),
				() -> clientNotificationService.sendMessage(
					senderSession, senderUserId, new ErrorResponse(
						MessageType.FETCH_USER_INVITE_CODE_REQUEST, "Fetch user invite code failed")));
	}
}
