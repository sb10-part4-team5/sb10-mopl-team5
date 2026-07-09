package com.codeit.team5.mopl.global.logging;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인증이 끝난 직후(컨트롤러 진입 직전) 시점에 userId를 MDC에 채워, 이후 같은 요청에서
 * 찍히는 모든 로그에 사용자 컨텍스트가 자동으로 붙게 한다.
 * MdcLoggingFilter보다 늦게 실행되므로(Spring Security 필터 체인 통과 후), 필터가 찍는
 * 최초 요청 로그 한 줄에는 userId가 붙지 않는다.
 */
@Component
public class MdcUserIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MoplPrincipal principal) {
            MDC.put(MdcKey.USER_ID, principal.getId().toString());
        }
        return true;
    }
}
