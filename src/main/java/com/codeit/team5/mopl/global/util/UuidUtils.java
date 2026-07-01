package com.codeit.team5.mopl.global.util;

import java.util.UUID;

public final class UuidUtils {

    private UuidUtils() {
    }

    // PostgreSQL uuid 비교(부호 없는 바이트 단위)와 일치하도록 unsigned 비교
    public static int compareUnsigned(UUID a, UUID b) {
        int high = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return high != 0 ? high
                : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    // unsigned 비교 기준으로 두 UUID 중 작은/큰 값
    public static UUID min(UUID a, UUID b) {
        return compareUnsigned(a, b) <= 0 ? a : b;
    }

    public static UUID max(UUID a, UUID b) {
        return compareUnsigned(a, b) <= 0 ? b : a;
    }
}
