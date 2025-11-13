package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.Constants;
import com.message.constant.MessageType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.FetchUserInviteCodeRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.FetchUserInviteCodeResponse;
import com.message.service.UserService;
import com.message.session.WebSocketSessionManager;

@Component
public class FetchUserInviteCodeRequestHandler implements BaseRequestHandler<FetchUserInviteCodeRequest> {

	private final UserService userService;
	private final WebSocketSessionManager webSocketSessionManager;

	public FetchUserInviteCodeRequestHandler(UserService userService,
		WebSocketSessionManager webSocketSessionManager) {
		this.userService = userService;
		this.webSocketSessionManager = webSocketSessionManager;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, FetchUserInviteCodeRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(Constants.USER_ID.getValue());
		userService.getInviteCode(senderUserId)
			.ifPresentOrElse(inviteCode -> webSocketSessionManager.sendMessage(senderSession,
					new FetchUserInviteCodeResponse(inviteCode)),
				() -> webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(
					MessageType.FETCH_USER_INVITE_CODE_REQUEST, "Fetch user invite code failed")));
	}
}
