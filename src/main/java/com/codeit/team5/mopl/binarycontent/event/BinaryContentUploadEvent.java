package com.codeit.team5.mopl.binarycontent.event;

import java.util.UUID;

public record BinaryContentUploadEvent(
        UUID contentId,
        String key,
        byte[] bytes
) {
    public BinaryContentUploadEvent {
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
