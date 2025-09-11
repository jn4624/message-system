package com.message.dto.domain;

/*
  record 클래스
  - 자바 16에서 추가된 클래스
  - 필드만 존재하면 Getter, toString, equals&hashcode 자동 생성
  - 불변 객체라 Setter는 미생성
 */
public record Message(String username, String content) {
}
