package com.codeit.team5.mopl.sse.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE", description = "Server-Sent Events API")
public interface SseApi {

    @Operation(operationId = "subscribe", summary = "SSE 구독",
            description = "실시간 알림 수신을 위한 SSE 연결을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공",
                    content = @Content(schema = @Schema(implementation = SseEmitter.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    SseEmitter subscribe(
            @Parameter(hidden = true) MoplPrincipal moplPrincipal,
            @Parameter(description = "마지막으로 수신한 이벤트 ID (재연결 시 사용)")
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId);
}
