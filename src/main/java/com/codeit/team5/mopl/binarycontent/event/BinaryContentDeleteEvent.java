package com.codeit.team5.mopl.binarycontent.event;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;

// 업로드 후 후속 작업 실패 시, 업로드된 스토리지 객체를 비동기로 삭제(롤백)하기 위한 이벤트
public record BinaryContentDeleteEvent(
        UploadedBinaryContent uploaded
) implements RetryableOutboxEvent {

}
