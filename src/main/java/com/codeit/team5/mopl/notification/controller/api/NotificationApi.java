package com.codeit.team5.mopl.notification.controller.api;

import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "알림 관리", description = "알림 API")
public interface NotificationApi {

    @Operation(operationId = "getNotifications", summary = "알림 목록 조회 (커서 페이지네이션)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CursorResponseNotificationDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(커서/정렬 값 오류 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<CursorResponseNotificationDto> getNotifications(
            // TODO: 인증 적용 후 @AuthenticationPrincipal MoplUserDetails 에서 receiverId 추출로 교체
            @Parameter(description = "수신자 ID (임시, 추후 인증 사용자에서 추출)", required = true)
            @RequestParam UUID receiverId,
            @Parameter(description = "다음 페이지 커서(createdAt)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "보조 커서(직전 페이지 마지막 ID)")
            @RequestParam(required = false) UUID idAfter,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "정렬 방향(ASCENDING/DESCENDING)")
            @RequestParam(defaultValue = "DESCENDING") String sortDirection,
            @Parameter(description = "정렬 기준(createdAt)")
            @RequestParam(defaultValue = "createdAt") String sortBy);

    @Operation(operationId = "readNotification", summary = "알림 읽음 처리",
            description = "알림을 읽음 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "알림 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> readNotification(
            // TODO: 인증 적용 후 @AuthenticationPrincipal MoplUserDetails 에서 receiverId 추출로 교체
            @Parameter(description = "수신자 ID (임시, 추후 인증 사용자에서 추출)", required = true)
            @RequestParam UUID receiverId,
            @Parameter(description = "알림 ID", required = true)
            @PathVariable UUID notificationId);
}
