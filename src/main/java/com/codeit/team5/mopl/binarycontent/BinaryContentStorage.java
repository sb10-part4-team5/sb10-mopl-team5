package com.codeit.team5.mopl.binarycontent;

import java.util.UUID;

public interface BinaryContentStorage {

    String generateKey(UUID contentId, String originalFilename);

    String toUrl(String key);

    void store(String key, byte[] bytes);
}
