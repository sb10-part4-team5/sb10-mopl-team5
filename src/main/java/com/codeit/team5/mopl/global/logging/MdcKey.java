package com.codeit.team5.mopl.global.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * MDC(Mapped Diagnostic Context) 및 추적 헤더에 사용하는 키 상수
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MdcKey {

    /** MDC에 저장하는 요청별 고유 ID 키 */
    public static final String REQUEST_ID = "requestId";

    /** MDC에 저장하는 클라이언트 IP 키 */
    public static final String CLIENT_IP = "clientIp";

    /** MDC에 저장하는 인증된 사용자 ID 키 (인증 이후 시점부터 채워짐) */
    public static final String USER_ID = "userId";

    /** 요청 추적 ID 전달/반환에 사용하는 HTTP 헤더명 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
}
