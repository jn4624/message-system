package com.message

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class MessageSystemApplicationSpec extends Specification {

	void "contextLoads"() {
        expect:
        true
	}
}
