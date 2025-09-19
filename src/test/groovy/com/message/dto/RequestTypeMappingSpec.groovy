package com.message.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.message.dto.websocket.inbound.BaseRequest
import com.message.dto.websocket.inbound.InviteRequest
import com.message.dto.websocket.inbound.KeepAliveRequest
import com.message.dto.websocket.inbound.WriteMessageRequest
import com.message.util.JsonUtil
import spock.lang.Specification


class RequestTypeMappingSpec extends Specification {

    JsonUtil jsonUtil = new JsonUtil(new ObjectMapper())

    def "DTO 형식의 JSON 문자열을 해당 타입의 DTO로 변환할 수 있다."() {
        given:
        String jsonBody = payload

        when:
        BaseRequest request = jsonUtil.fromJson(jsonBody, BaseRequest).get()

        then:
        request.getClass() == expectedClass
        validate(request)

        where:
        payload                                                                        | expectedClass       | validate
        '{"type": "INVITE_REQUEST", "userInviteCode": "TestInviteCode123"}'            | InviteRequest       | { req -> (req as InviteRequest).userInviteCode.code() == "TestInviteCode123" }
        '{"type": "WRITE_MESSAGE", "username": "testuser", "content": "test message"}' | WriteMessageRequest | { req -> (req as WriteMessageRequest).content == "test message" }
        '{"type": "KEEP_ALIVE"}'                                                       | KeepAliveRequest    | { req -> (req as KeepAliveRequest).type == "KEEP_ALIVE" }
    }
}