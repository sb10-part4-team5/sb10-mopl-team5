package com.codeit.team5.mopl.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidUtilsTest {

    @Test
    @DisplayName("MSB가 다르면 unsigned 비교로 min/max 판별 성공")
    void minMax_differentMsb_success() {
        UUID small = new UUID(1L, 0L);
        UUID large = new UUID(2L, 0L);
        assertThat(UuidUtils.min(small, large)).isEqualTo(small);
        assertThat(UuidUtils.max(small, large)).isEqualTo(large);
    }

    @Test
    @DisplayName("MSB가 같고 LSB만 다르면 unsigned 비교로 min/max 판별 성공")
    void minMax_sameMsbDifferentLsb_success() {
        UUID a = new UUID(1L, Long.MIN_VALUE); // unsigned 관점에서 큰 값
        UUID b = new UUID(1L, 0L);
        assertThat(UuidUtils.min(a, b)).isEqualTo(b);
        assertThat(UuidUtils.max(a, b)).isEqualTo(a);
    }

    @Test
    @DisplayName("동일한 UUID면 min과 max가 같은 값 반환 성공")
    void minMax_sameUuid_success() {
        UUID uuid = UUID.randomUUID();
        assertThat(UuidUtils.min(uuid, uuid)).isEqualTo(uuid);
        assertThat(UuidUtils.max(uuid, uuid)).isEqualTo(uuid);
    }
}
