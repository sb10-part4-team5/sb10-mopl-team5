package com.codeit.team5.mopl.binarycontent.event;

import java.util.UUID;

public record BinaryContentUploadEvent(
        UUID binaryContentId,
        String key,
        byte[] bytes
) {
}
