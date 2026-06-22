package com.codeit.team5.mopl.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 추적용 requestId와 clientIp를 MDC에 채우고, 응답 헤더에 requestId를 내려주는 필터
 * Spring Security 필터 체인(기본 order -100)보다 먼저 실행되도록 최우선 순서로 등록
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    /** 외부에서 들어온 requestId 재사용 시 허용하는 최대 길이 */
    private static final int MAX_REQUEST_ID_LENGTH = 64;

    /** 로그 인젝션 방지를 위해 영숫자/하이픈만 허용 */
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9\\-]+");

    @Override
    protected void doFilterInternal (
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String requestId = resolveRequestId(request);
            MDC.put(MdcKey.REQUEST_ID, requestId);
            MDC.put(MdcKey.CLIENT_IP, resolveClientIp(request));

            // 클라이언트가 응답의 ID로 서버 로그를 추적할 수 있도록 헤더로 반환
            response.setHeader(MdcKey.REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            // 스레드 풀 재사용 시 다음 요청에 값이 새지 않도록 정리
            MDC.clear();
        }
    }

    /**
     * 외부에서 유효한 X-Request-Id가 들어오면 재사용
     * 없거나 형식이 안전하지 않으면 새로 생성한다.
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(MdcKey.REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId) && isSafeRequestId(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private boolean isSafeRequestId(String requestId) {
        return requestId.length() <= MAX_REQUEST_ID_LENGTH
            && SAFE_REQUEST_ID.matcher(requestId).matches();
    }

    /**
     * 클라이언트 IP 추출.
     * EC2 + 로드밸런서(ALB) 환경을 고려하여
     * X-Forwarded-For -> X-Real-IP -> remoteAddr 순으로 확인
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            // "client, proxy1, proxy2" 형태로 들어오므로 맨 앞이 원 클라이언트
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
