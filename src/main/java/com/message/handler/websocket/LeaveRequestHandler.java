package com.message.handler.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.message.constant.IdKey;
import com.message.constant.MessageType;
import com.message.dto.domain.UserId;
import com.message.dto.websocket.inbound.LeaveRequest;
import com.message.dto.websocket.outbound.ErrorResponse;
import com.message.dto.websocket.outbound.LeaveResponse;
import com.message.service.ChannelService;
import com.message.service.ClientNotificationService;

@Component
public class LeaveRequestHandler implements BaseRequestHandler<LeaveRequest> {

	private final ChannelService channelService;
	private final ClientNotificationService clientNotificationService;

	public LeaveRequestHandler(
		ChannelService channelService,
		ClientNotificationService clientNotificationService
	) {
		this.channelService = channelService;
		this.clientNotificationService = clientNotificationService;
	}

	@Override
	public void handleRequest(WebSocketSession senderSession, LeaveRequest request) {
		UserId senderUserId = (UserId)senderSession.getAttributes().get(IdKey.USER_ID.getValue());

		if (channelService.leave(senderUserId)) {
			clientNotificationService.sendMessage(senderSession, senderUserId, new LeaveResponse());
		} else {
			clientNotificationService.sendMessage(
				senderSession, senderUserId, new ErrorResponse(MessageType.LEAVE_REQUEST, "Leave failed"));
		}
	}
}
