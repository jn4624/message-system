package com.message.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.message.constant.MessageType;

// 정의된 타입을 가지고 서브 클래스인 json을 객체로 변환할 때 맞는 객체로 변환 가능
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = FetchUserInviteCodeRequest.class, name = MessageType.FETCH_USER_INVITE_CODE_REQUEST),
	@JsonSubTypes.Type(value = FetchConnectionsRequest.class, name = MessageType.FETCH_CONNECTIONS_REQUEST),
	@JsonSubTypes.Type(value = InviteRequest.class, name = MessageType.INVITE_REQUEST),
	@JsonSubTypes.Type(value = AcceptRequest.class, name = MessageType.ACCEPT_REQUEST),
	@JsonSubTypes.Type(value = RejectRequest.class, name = MessageType.REJECT_REQUEST),
	@JsonSubTypes.Type(value = DisconnectRequest.class, name = MessageType.DISCONNECT_REQUEST),
	@JsonSubTypes.Type(value = CreateRequest.class, name = MessageType.CREATE_REQUEST),
	@JsonSubTypes.Type(value = EnterRequest.class, name = MessageType.ENTER_REQUEST),
	@JsonSubTypes.Type(value = WriteMessage.class, name = MessageType.WRITE_MESSAGE),
	@JsonSubTypes.Type(value = KeepAlive.class, name = MessageType.KEEP_ALIVE)
})
public abstract class BaseRequest {

	private final String type;

	public BaseRequest(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
