package com.codeit.team5.mopl.global.entity;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@MappedSuperclass // 테이블로 사용되지는 못하고, 해당 인터페이스를 상속받는 객체가 테이블로 생성됨
@RequiredArgsConstructor
public abstract class BaseUpdatableEntity extends BaseEntity{
    @LastModifiedDate
    private Instant updatedAt;
}
