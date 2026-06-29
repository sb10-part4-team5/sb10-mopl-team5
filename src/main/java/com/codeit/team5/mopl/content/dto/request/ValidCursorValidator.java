package com.codeit.team5.mopl.content.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;
import java.util.UUID;

public class ValidCursorValidator implements ConstraintValidator<ValidCursor, ContentCursorRequest> {

    @Override
    public boolean isValid(ContentCursorRequest request, ConstraintValidatorContext context) {
        String cursor = request.cursor();
        String idAfter = request.idAfter();

        if ((cursor == null) ^ (idAfter == null)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("cursor와 idAfter가 같이 제공되지 않습니다.")
                    .addConstraintViolation();
            return false;
        }

        if (cursor == null) return true;

        try {
            UUID.fromString(idAfter);
        } catch (IllegalArgumentException e) {
            return false;
        }

        try {
            switch (request.sortBy()) {
                case CREATED_AT -> Instant.parse(cursor);
                case WATCHER_COUNT -> Long.parseLong(cursor);
                case RATE -> {
                    double val = Double.parseDouble(cursor);
                    if (!Double.isFinite(val)) return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
