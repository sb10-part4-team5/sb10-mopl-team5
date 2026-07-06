package com.codeit.team5.mopl.auth.security.handler;

import com.codeit.team5.mopl.auth.support.ErrorResponder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("Access denied: method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getMessage());

        ErrorResponder.sendErrorResponse(response, accessDeniedException);
    }
}
