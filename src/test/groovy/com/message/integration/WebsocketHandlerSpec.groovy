package com.message.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.message.MessageSystemApplication
import com.message.dto.domain.ChannelId
import com.message.dto.websocket.inbound.WriteMessage
import com.message.service.ChannelService
import com.message.service.UserService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(
        classes = MessageSystemApplication,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class WebsocketHandlerSpec extends Specification {

    /*
      - 테스트 코드에 선언하는 필드에 private 접근 제어자를 붙이지 말 것.
      - 테스트 코드에 선언된 필드는 테스트 클래스에서만 접근하기 때문에 private 접근 제어자를 붙이지 않아도 된다.
      - 접근 제어자를 사용하지 않는 것이 권장 가이드다.
     */
    @LocalServerPort
    int port

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserService userService

    @SpringBean
    ChannelService channelService = Stub()

    def "Group Chat Basic Test"() {
        given:
        register("testuserA", "testpassA")
        register("testuserB", "testpassB")
        register("testuserC", "testpassC")

        def sessionIdA = login("testuserA", "testpassA")
        def sessionIdB = login("testuserB", "testpassB")
        def sessionIdC = login("testuserC", "testpassC")

        // tuple = list - 1대1로 매칭시켜서 각각 할당 됨
        def (clientA, clientB, clientC) = [createClient(sessionIdA), createClient(sessionIdB), createClient(sessionIdC)]

        channelService.getOnlineParticipantIds(_ as ChannelId) >> List.of(
                userService.getUserId("testuserA").get(),
                userService.getUserId("testuserB").get(),
                userService.getUserId("testuserC").get())

        when:
        clientA.session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. A 입니다."))))
        clientB.session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. B 입니다."))))
        clientC.session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. C 입니다."))))

        then:
        def resultA = clientA.queue.poll(1, TimeUnit.SECONDS) + clientA.queue.poll(1, TimeUnit.SECONDS)
        def resultB = clientB.queue.poll(1, TimeUnit.SECONDS) + clientB.queue.poll(1, TimeUnit.SECONDS)
        def resultC = clientC.queue.poll(1, TimeUnit.SECONDS) + clientC.queue.poll(1, TimeUnit.SECONDS)

        resultA.contains("testuserB") && resultA.contains("testuserC")
        resultB.contains("testuserA") && resultB.contains("testuserC")
        resultC.contains("testuserA") && resultC.contains("testuserB")

        and:
        clientA.queue.isEmpty()
        clientB.queue.isEmpty()
        clientC.queue.isEmpty()

        cleanup:
        unregister(sessionIdA)
        unregister(sessionIdB)
        unregister(sessionIdC)

        clientA.session?.close()
        clientB.session?.close()
        clientC.session?.close()
    }

    def register(String username, String password) {
        def url = "http://localhost:${port}/api/v1/auth/register"
        def headers = new HttpHeaders(["Content-Type": "application/json"])
        def jsonBody = objectMapper.writeValueAsString([username: username, password: password])
        def httpEntity = new HttpEntity(jsonBody, headers)

        try {
            new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        } catch (Exception ignored) {
            // 이미 등록된 사용자일 경우 예외가 발생할 수 있으므로 무시
        }
    }

    def unregister(String sessionId) {
        def url = "http://localhost:${port}/api/v1/auth/unregister"
        def headers = new HttpHeaders()
        headers.add("Content-Type", "application/json")
        headers.add("Cookie", "SESSION=$sessionId")
        def httpEntity = new HttpEntity(headers)

        def responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        responseEntity.body
    }

    def login(String username, String password) {
        def url = "http://localhost:${port}/api/v1/auth/login"
        def headers = new HttpHeaders(["Content-Type": "application/json"])
        def jsonBody = objectMapper.writeValueAsString([username: username, password: password])
        def httpEntity = new HttpEntity(jsonBody, headers)

        def responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        def sessionId = responseEntity.body
        sessionId
    }

    def createClient(String sessionId) {
        def url = "ws://localhost:${port}/ws/v1/message"
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(5)
        def webSocketHttpHeaders = new WebSocketHttpHeaders()
        webSocketHttpHeaders.add("Cookie", "SESSION=$sessionId")

        def client = new StandardWebSocketClient()
        def webSocketSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                blockingQueue.put(message.payload)
            }
        }, webSocketHttpHeaders, new URI(url)).get()

        // groovy map return
        [queue: blockingQueue, session: webSocketSession]
    }
}
