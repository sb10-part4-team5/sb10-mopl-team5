package com.codeit.team5.mopl.content.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPageRangeValidator implements ConstraintValidator<ValidPageRange, PageRangeRequest> {

    @Override
    public boolean isValid(PageRangeRequest request, ConstraintValidatorContext context) {
        // null/누락, startPage >= 1 여부는 @NotNull/@Min(1)이 이미 검사하므로 여기서는
        // endPage < startPage 같은 교차 필드 조건만 검사한다.
        if (request == null || request.startPage() == null || request.endPage() == null) {
            return true;
        }
        if (request.endPage() >= request.startPage()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endPage")
                .addConstraintViolation();
        return false;
    }
}
