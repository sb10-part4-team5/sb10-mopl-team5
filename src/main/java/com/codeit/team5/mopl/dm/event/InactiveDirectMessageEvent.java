package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;

// 비활성 대화(수신자가 보고 있지 않은 대화)에 DM이 도착했음을 알리는 이벤트
// 알림 저장 리스너와 SSE 전송 리스너가 각각 독립적으로 구독한다.
public record InactiveDirectMessageEvent(
        DirectMessageResponse message
) {
}
