package com.codeit.team5.mopl.global.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@RequiredArgsConstructor
public abstract class BaseUpdatableEntity extends BaseEntity{
    @LastModifiedDate
    private Instant updatedAt;
}
