package com.codeit.team5.mopl.binarycontent.storage;

public interface BinaryContentStorage {

    String toUrl(String key);

    void store(String key, byte[] bytes, String contentType);
}
