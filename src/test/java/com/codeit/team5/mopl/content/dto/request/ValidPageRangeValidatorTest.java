package com.codeit.team5.mopl.content.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidPageRangeValidatorTest {

    private ValidPageRangeValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ValidPageRangeValidator();
        context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    @DisplayName("startPage <= endPage면 유효하다")
    void valid_endPageGreaterThanOrEqualToStartPage() {
        PageRangeRequest request = new PageRangeRequest(1, 5);

        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    @DisplayName("startPage == endPage면 유효하다")
    void valid_startPageEqualsEndPage() {
        PageRangeRequest request = new PageRangeRequest(3, 3);

        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    @DisplayName("endPage가 startPage보다 작으면 유효하지 않고, 위반이 endPage 필드에 귀속된다")
    void invalid_endPageLessThanStartPage() {
        PageRangeRequest request = new PageRangeRequest(5, 3);

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()))
                .addPropertyNode("endPage");
    }

    @Test
    @DisplayName("startPage가 0 이하여도 이 검증기는 관여하지 않는다 (@Min(1)의 책임)")
    void valid_startPageBelowOne_notCheckedHere() {
        // startPage < 1은 @Min(1)이 검사할 몫이므로, 이 검증기는 endPage >= startPage만 보고 유효로 판단한다.
        PageRangeRequest request = new PageRangeRequest(0, 1);

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("startPage 또는 endPage가 null이면 유효하다 (@NotNull의 책임)")
    void valid_nullFields_notCheckedHere() {
        PageRangeRequest request = new PageRangeRequest(null, null);

        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    @DisplayName("request가 null이면 유효하다")
    void valid_nullRequest() {
        assertThat(validator.isValid(null, context)).isTrue();
    }
}
