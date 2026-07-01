package com.codeit.team5.mopl.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidUtilsTest {

    @Test
    @DisplayName("MSB가 다르면 unsigned 비교로 정렬 성공")
    void sorted_differentMsb_success() {
        UUID small = new UUID(1L, 0L);
        UUID large = new UUID(2L, 0L);
        assertThat(UuidUtils.sorted(small, large)).containsExactly(small, large);
        assertThat(UuidUtils.sorted(large, small)).containsExactly(small, large);
    }

    @Test
    @DisplayName("MSB가 같고 LSB만 다르면 unsigned 비교로 정렬 성공")
    void sorted_sameMsbDifferentLsb_success() {
        UUID a = new UUID(1L, Long.MIN_VALUE); // unsigned 관점에서 큰 값
        UUID b = new UUID(1L, 0L);
        assertThat(UuidUtils.sorted(a, b)).containsExactly(b, a);
    }

    @Test
    @DisplayName("동일한 UUID면 정렬 결과도 동일 성공")
    void sorted_sameUuid_success() {
        UUID uuid = UUID.randomUUID();
        assertThat(UuidUtils.sorted(uuid, uuid)).containsExactly(uuid, uuid);
    }

    @Test
    @DisplayName("부호 없는 바이트 기준으로 대소를 비교 성공")
    void compareUnsigned_success() {
        UUID small = new UUID(1L, 0L);
        UUID large = new UUID(2L, 0L);
        assertThat(UuidUtils.compareUnsigned(small, large)).isNegative();
        assertThat(UuidUtils.compareUnsigned(large, small)).isPositive();
        assertThat(UuidUtils.compareUnsigned(small, small)).isZero();

        // 음수 long은 unsigned 관점에서 가장 큰 값
        UUID unsignedLargest = new UUID(Long.MIN_VALUE, 0L);
        assertThat(UuidUtils.compareUnsigned(unsignedLargest, large)).isPositive();
    }
}
