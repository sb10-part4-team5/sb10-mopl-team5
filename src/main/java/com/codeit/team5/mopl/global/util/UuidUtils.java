package com.codeit.team5.mopl.global.util;

import java.util.UUID;

public final class UuidUtils {

    private UuidUtils() {
    }

    // PostgreSQL uuid 비교(부호 없는 바이트 단위)와 일치하도록 unsigned 비교한다.
    public static int compareUnsigned(UUID a, UUID b) {
        int high = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return high != 0 ? high
                : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }
}
