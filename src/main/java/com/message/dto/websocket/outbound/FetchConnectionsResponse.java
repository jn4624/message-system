package com.message.dto.websocket.outbound;

import java.util.List;

import com.message.constant.MessageType;
import com.message.dto.domain.Connection;

public class FetchConnectionsResponse extends BaseMessage {

	private final List<Connection> connections;

	public FetchConnectionsResponse(List<Connection> connections) {
		super(MessageType.FETCH_CONNECTIONS_RESPONSE);
		this.connections = connections;
	}

	public List<Connection> getConnections() {
		return connections;
	}
}
