package com.codeit.team5.mopl.binarycontent;

public interface BinaryContentStorage {

    String toUrl(String key);

    void store(String key, byte[] bytes, String contentType);
}
