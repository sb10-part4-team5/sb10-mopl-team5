package com.codeit.team5.mopl.binarycontent;

/**
 * 저장소 키의 최상위 폴더(prefix). 용도별로 추가하려면 상수만 추가하면 된다.
 */
public enum StorageDirectory {
    THUMBNAIL("thumbnails"),
    PROFILE("profiles");

    private final String value;

    StorageDirectory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
