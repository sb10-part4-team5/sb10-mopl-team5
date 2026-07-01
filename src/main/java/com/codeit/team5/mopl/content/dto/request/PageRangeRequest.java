package com.codeit.team5.mopl.content.dto.request;

import org.springframework.web.bind.annotation.RequestParam;

@ValidPageRange
public record PageRangeRequest(
        @RequestParam(defaultValue = "1") int startPage,
        @RequestParam(defaultValue = "1") int endPage
) {}
