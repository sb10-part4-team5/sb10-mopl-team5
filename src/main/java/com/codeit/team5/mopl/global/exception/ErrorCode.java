package com.codeit.team5.mopl.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_ROLE_CHANGE(HttpStatus.CONFLICT, "현재 사용자의 역할과 변경할 역할이 동일합니다."),
    LOCK_STATUS_ALREADY_SET(HttpStatus.CONFLICT, "현재 사용자의 잠금 상태와 변경할 잠금 상태가 동일합니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "사용자 이름은 공백일 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "잠긴 계정입니다."),

    // Social Auth
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연결된 소셜 계정입니다."),

    // Content
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 콘텐츠에 리뷰를 작성했습니다."),

    // Playlist
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트를 찾을 수 없습니다."),
    PLAYLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트 항목을 찾을 수 없습니다."),
    PLAYLIST_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 플레이리스트에 추가된 콘텐츠입니다."),

    // Follow
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 팔로우 중입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우 관계를 찾을 수 없습니다."),
    SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),

    // Watching Session
    WATCHING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "시청 세션을 찾을 수 없습니다."),

    // Conversation / DM
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
