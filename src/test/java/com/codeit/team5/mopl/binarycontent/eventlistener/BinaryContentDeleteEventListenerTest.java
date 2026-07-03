package com.codeit.team5.mopl.binarycontent.eventlistener;

import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentDeleteEvent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BinaryContentDeleteEventListenerTest {

    @Mock
    private BinaryContentService binaryContentService;

    @InjectMocks
    private BinaryContentDeleteEventListener listener;

    @Test
    @DisplayName("삭제 이벤트를 받으면 스토리지 객체 삭제 성공")
    void handle_deletesUploaded() {
        // given
        UploadedBinaryContent uploaded =
                new UploadedBinaryContent("thumbnails/key.jpg", "http://localhost/thumbnails/key.jpg");

        // when
        listener.handle(new BinaryContentDeleteEvent(uploaded));

        // then
        verify(binaryContentService).deleteQuietly(uploaded);
    }
}
