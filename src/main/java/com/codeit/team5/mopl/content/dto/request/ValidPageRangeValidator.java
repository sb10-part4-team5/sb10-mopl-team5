package com.codeit.team5.mopl.content.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPageRangeValidator implements ConstraintValidator<ValidPageRange, PageRangeRequest> {

    @Override
    public boolean isValid(PageRangeRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;
        return request.startPage() >= 1 && request.endPage() >= request.startPage();
    }
}
