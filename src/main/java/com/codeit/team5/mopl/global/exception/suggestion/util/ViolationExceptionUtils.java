package com.codeit.team5.mopl.global.exception.suggestion.util;

import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ViolationExceptionUtils {

    public static boolean isFromController(ConstraintViolationException ex) {
        if (ex == null || ex.getConstraintViolations() == null) {
            return false;
        }
        return ex.getConstraintViolations().stream()
                .anyMatch(v -> v.getRootBeanClass().getSimpleName().endsWith("Controller")
                        || v.getRootBeanClass().isAnnotationPresent(RestController.class));
    }

}
