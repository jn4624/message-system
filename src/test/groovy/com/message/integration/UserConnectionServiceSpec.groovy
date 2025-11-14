package com.message.integration

import com.message.MessageSystemApplication
import com.message.constant.UserConnectionStatus
import com.message.dto.domain.UserId
import com.message.entity.UserConnectionId
import com.message.repository.UserConnectionRepository
import com.message.repository.UserRepository
import com.message.service.UserConnectionLimitService
import com.message.service.UserConnectionService
import com.message.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = MessageSystemApplication)
class UserConnectionServiceSpec extends Specification {

    @Autowired
    UserService userService

    @Autowired
    UserConnectionService userConnectionService

    @Autowired
    UserConnectionLimitService userConnectionLimitService

    @Autowired
    UserRepository userRepository

    @Autowired
    UserConnectionRepository userConnectionRepository

    def "연결 요청 수락은 연결 제한 수를 넘을 수 없다."() {
        given:
        userConnectionLimitService.setLimitConnections(10)
        (0..19).each { userService.addUser("testuser${it}", "testpass${it}") }
        def userIdA = userService.getUserId("testuser0").get()
        def inviteCodeA = userService.getInviteCode(userIdA).get()

        (1..9).each {
            userConnectionService.invite(userService.getUserId("testuser${it}").get(), inviteCodeA)
            userConnectionService.accept(userIdA, "testuser${it}")
        }

        def inviteCodes = (10..19).collect {
            userService.getInviteCode(userService.getUserId("testuser${it}").get()).get()
        }

        inviteCodes.each { userConnectionService.invite(userIdA, it) }
        def results = Collections.synchronizedList(new ArrayList<Optional<UserId>>())

        when:
        def threads = (10..19).collect { idx ->
            Thread.start {
                def userId = userService.getUserId("testuser${idx}")
                results << userConnectionService.accept(userId.get(), "testuser0").getFirst()
            }
        }
        threads*.join()

        then:
        results.count { it.isPresent() } == 1
    }

    def "연결 요청 카운트는 0보다 작을 수 없다."() {
        given:
        (0..10).each { userService.addUser("testuser${it}", "testpass${it}") }
        def userIdA = userService.getUserId("testuser0").get()
        def inviteCodeA = userService.getInviteCode(userIdA).get()

        (1..10).each {
            userConnectionService.invite(userService.getUserId("testuser${it}").get(), inviteCodeA)
        }
        (1..5).each {
            userConnectionService.accept(userIdA, "testuser${it}")
        }

        def results = Collections.synchronizedList(new ArrayList<Boolean>())

        when:
        def threads = (1..10).collect { idx ->
            Thread.start {
                def userId = userService.getUserId("testuser${idx}")
                results << userConnectionService.disconnect(userId.get(), "testuser0").getFirst()
            }
        }
        threads*.join()

        then:
        results.count { it == true } == 5
        userService.getConnectionCount(userService.getUserId("testuser0").get()).get() == 0
    }

    def cleanup() {
        (0..19).each {
            userService.getUserId("testuser${it}").ifPresent { userId ->
                userRepository.deleteById(userId.id())

                userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), UserConnectionStatus.PENDING).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId(),), Long.max(userId.id(), it.getUserId())))
                }
                userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), UserConnectionStatus.PENDING).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId(),), Long.max(userId.id(), it.getUserId())))
                }

                userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), UserConnectionStatus.ACCEPTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId(),), Long.max(userId.id(), it.getUserId())))
                }
                userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), UserConnectionStatus.ACCEPTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId(),), Long.max(userId.id(), it.getUserId())))
                }

                userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), UserConnectionStatus.DISCONNECTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId(),), Long.max(userId.id(), it.getUserId())))
                }
                userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), UserConnectionStatus.DISCONNECTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId(),), Long.max(userId.id(), it.getUserId())))
                }
            }
        }
    }
}
