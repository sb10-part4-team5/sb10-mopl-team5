package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.notification.exception.InvalidContentException;
import com.codeit.team5.mopl.notification.exception.InvalidNicknameException;
import java.util.Objects;
import java.util.UUID;

// DM을 발송하는 주체쪽에서 각 파라미터들을 주입하는 식으로 작성하였습니다.
// 추후 DM 객체가 작성된다면 논의 후 수정 예정입니다.
public record DirectMessageSentEvent (
    UUID receiverId, // 수신자 ID
    String senderNickname, // 송신자 닉네임
    String content // 메시지 내용
){
    public DirectMessageSentEvent {
        Objects.requireNonNull(receiverId, "receiverId가 유효하지 않음");
        if (senderNickname == null || senderNickname.isBlank()) {
            throw new InvalidNicknameException(senderNickname);
        }
        if (content == null || content.isBlank()){
            throw new InvalidContentException();
        }
    }
}
