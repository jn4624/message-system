package com.message.service

import com.message.constant.UserConnectionStatus
import com.message.dto.domain.InviteCode
import com.message.dto.domain.User
import com.message.dto.domain.UserId
import com.message.dto.projection.UserConnectionStatusProjection
import com.message.repository.UserConnectionRepository
import org.springframework.data.util.Pair
import spock.lang.Specification


class UserConnectionServiceSpec extends Specification {

    UserConnectionService userConnectionService
    UserService userService = Stub()
    UserConnectionRepository userConnectionRepository = Stub()

    def setup() {
        userConnectionService = new UserConnectionService(userService, userConnectionRepository)
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
}