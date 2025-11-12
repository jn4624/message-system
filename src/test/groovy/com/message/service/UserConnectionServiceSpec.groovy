package com.message.service

import com.message.constant.UserConnectionStatus
import com.message.dto.domain.InviteCode
import com.message.dto.domain.User
import com.message.dto.domain.UserId
import com.message.dto.projection.InviterUserIdProjection
import com.message.dto.projection.UserConnectionStatusProjection
import com.message.entity.UserConnectionEntity
import com.message.entity.UserEntity
import com.message.repository.UserConnectionRepository
import com.message.repository.UserRepository
import org.springframework.data.util.Pair
import spock.lang.Specification


class UserConnectionServiceSpec extends Specification {

    UserConnectionService userConnectionService
    UserConnectionLimitService userConnectionLimitService
    UserService userService = Stub()
    UserRepository userRepository = Stub()
    UserConnectionRepository userConnectionRepository = Stub()

    def setup() {
        userConnectionLimitService = new UserConnectionLimitService(userRepository, userConnectionRepository)
        userConnectionService = new UserConnectionService(userService, userConnectionLimitService, userConnectionRepository)
    }

    def "사용자 연결 신청에 대한 테스트."() {
        given:
        userService.getUser(inviteCodeOfTargetUser) >> Optional.of(new User(tergetUserId, targetUsername))
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        when:
        def result = userConnectionService.invite(senderUserId, usedInviteCode)

        then:
        result == expectedResult

        where:
        scenario              | senderUserId  | senderUsername | tergetUserId  | targetUsername | inviteCodeOfTargetUser      | usedInviteCode                | beforeConnectionStatus            | expectedResult
        'Valid invite code'   | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.NONE         | Pair.of(Optional.of(new UserId(2)), 'userA')
        'Already connected'   | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.ACCEPTED     | Pair.of(Optional.empty(), 'Already connected with ' + targetUsername)
        'Already invited'     | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), 'Already invited to ' + targetUsername)
        'Already rejected'    | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.REJECTED     | Pair.of(Optional.empty(), 'Already invited to ' + targetUsername)
        'After disconnected'  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.of(new UserId(2)), 'userA')
        'Invalid invite code' | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('nobody code') | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), 'Invalid invite code')
        'Self invite'         | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | new InviteCode('user1code') | new InviteCode('user1code')   | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), "Can't self invite")
    }

    def "사용자 연결 신청 수락에 대한 테스트."() {
        given:
        userService.getUserId(targetUsername) >> Optional.of(tergetUserId)
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)
        userRepository.findForUpdateByUserId(_ as Long) >> { Long userId ->
            def entity = new UserEntity()
            if (userId == 5 || userId == 7) {
                entity.setConnectionCount(1_000)
            }
            return Optional.of(entity)
        }
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserIdAndStatus(_ as Long, _ as Long, _ as UserConnectionStatus) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(new UserConnectionEntity(senderUserId.id(), tergetUserId.id(), UserConnectionStatus.PENDING, inviter.id()))
            }
        }
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }
        userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(Stub(InviterUserIdProjection) {
                    getInviterUserId() >> inviter.id()
                })
            }
        }

        when:
        def result = userConnectionService.accept(senderUserId, targetUsername)

        then:
        result == expectedResult

        where:
        scenario                          | senderUserId  | senderUsername | tergetUserId  | targetUsername | inviterUserId              | beforeConnectionStatus            | expectedResult
        'Accept invite'                   | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(Optional.of(new UserId(2)), 'userA')
        'Already Connected'               | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.ACCEPTED     | Pair.of(Optional.empty(), 'Already connected')
        'Self accept'                     | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Can't self accept")
        'Accept wrong invite'             | new UserId(1) | 'userA'        | new UserId(4) | 'userD'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Invalid username")
        'Accept invalid invite'           | new UserId(1) | 'userA'        | new UserId(4) | 'userD'        | Optional.empty()           | UserConnectionStatus.NONE         | Pair.of(Optional.empty(), "Invalid username")
        'After rejected'                  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.REJECTED     | Pair.of(Optional.empty(), "Accept failed")
        'After disconnected'              | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), "Accept failed")
        'Limit reached'                   | new UserId(5) | 'userE'        | new UserId(6) | 'userF'        | Optional.of(new UserId(6)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Connection limit reached")
        'Limit reached by the other user' | new UserId(8) | 'userI'        | new UserId(7) | 'userH'        | Optional.of(new UserId(7)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Connection limit reached by the other user")
    }
}
