package com.codeit.team5.mopl.binarycontent.eventlistener;

import com.codeit.team5.mopl.binarycontent.event.BinaryContentDeleteEvent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BinaryContentDeleteEventListener {

    private final BinaryContentService binaryContentService;

    @Async("binaryContentTaskExecutor")
    @EventListener
    public void handle(BinaryContentDeleteEvent event) {
        binaryContentService.deleteQuietly(event.uploaded());
    }
}
