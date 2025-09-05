package com.message.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.message.dto.Message
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageHandlerSpec extends Specification {

    /*
      - 테스트 코드에 선언하는 필드에 private 접근 제어자를 붙이지 말 것.
      - 테스트 코드에 선언된 필드는 테스트 클래스에서만 접근하기 때문에 private 접근 제어자를 붙이지 않아도 된다.
      - 접근 제어자를 사용하지 않는 것이 권장 가이드다.
     */
    @LocalServerPort
    int port

    ObjectMapper objectMapper = new ObjectMapper()

    def "Group Chat Basic Test"() {
        given:
        def url = "ws://localhost:${port}/ws/v1/message"
        // tuple = list - 1대1로 매칭시켜서 각각 할당 됨
        def (clientA, clientB, clientC) = [createClient(url), createClient(url), createClient(url)]

        when:
        clientA.session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new Message("clientA", "안녕하세요. A 입니다."))))
        clientB.session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new Message("clientB", "안녕하세요. B 입니다."))))
        clientC.session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new Message("clientC", "안녕하세요. C 입니다."))))

        then:
        def resultA = clientA.queue.poll(1, TimeUnit.SECONDS) + clientA.queue.poll(1, TimeUnit.SECONDS)
        def resultB = clientB.queue.poll(1, TimeUnit.SECONDS) + clientB.queue.poll(1, TimeUnit.SECONDS)
        def resultC = clientC.queue.poll(1, TimeUnit.SECONDS) + clientC.queue.poll(1, TimeUnit.SECONDS)

        resultA.contains("clientB") && resultA.contains("clientC")
        resultB.contains("clientA") && resultB.contains("clientC")
        resultC.contains("clientA") && resultC.contains("clientB")

        and:
        clientA.queue.isEmpty()
        clientB.queue.isEmpty()
        clientC.queue.isEmpty()

        cleanup:
        clientA.session?.close()
        clientB.session?.close()
        clientC.session?.close()
    }

    static def createClient(String url) {
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(5)

        def client = new StandardWebSocketClient()
        def webSocketSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                blockingQueue.put(message.payload)
            }
        }, url).get()

        // groovy map return
        [queue: blockingQueue, session: webSocketSession]
    }
}
