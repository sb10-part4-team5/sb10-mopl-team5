package com.codeit.team5.mopl.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 추적용 requestId와 clientIp를 MDC에 채우고, 응답 헤더에 requestId를 내려주는 필터
 * Spring Security 필터 체인(기본 order -100)보다 먼저 실행되도록 최우선 순서로 등록
 * 요청 최초 로그도 컨트롤러마다 중복으로 남기지 않고 여기서 한 번만 기록한다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String HEALTH_CHECK_PATH = "/actuator/health";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String requestId = UUID.randomUUID().toString();
            MDC.put(MdcKey.REQUEST_ID, requestId);
            MDC.put(MdcKey.CLIENT_IP, request.getRemoteAddr());

            // 클라이언트가 응답의 ID로 서버 로그를 추적할 수 있도록 헤더로 반환
            response.setHeader(MdcKey.REQUEST_ID_HEADER, requestId);

            String requestURI = request.getRequestURI();
            if (!requestURI.equals(HEALTH_CHECK_PATH) && !requestURI.startsWith(HEALTH_CHECK_PATH + "/")) {
                log.info("{} {}", request.getMethod(), requestURI);
            }

            filterChain.doFilter(request, response);
        } finally {
            // 스레드 풀 재사용 시 다음 요청에 값이 새지 않도록 정리
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
