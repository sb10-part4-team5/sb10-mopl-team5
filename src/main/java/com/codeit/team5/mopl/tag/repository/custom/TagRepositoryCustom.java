package com.codeit.team5.mopl.tag.repository.custom;

import java.util.List;

public interface TagRepositoryCustom {

    /**
     * 이름이 겹치는 태그는 건너뛰고 없는 태그만 삽입한다 (동시 요청 간 유니크 제약 충돌을 예외 없이 처리).
     */
    void insertIfAbsent(List<String> names);
}
