package com.codeit.team5.mopl.dm.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "DM 메시지 관리", description = "다이렉트 메시지 조회/읽음 API")
public interface DirectMessageApi {

    @Operation(operationId = "getDirectMessages", summary = "대화 메시지 목록 조회",
            description = "특정 대화의 메시지 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "대화 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Parameters({
            @Parameter(name = "cursor", description = "커서"),
            @Parameter(name = "idAfter", description = "보조 커서"),
            @Parameter(name = "limit", description = "한 번에 가져올 개수", example = "20", required = true),
            @Parameter(name = "sortDirection", description = "정렬 방향", required = true,
                    schema = @Schema(type = "string", allowableValues = {"ASCENDING", "DESCENDING"},
                            defaultValue = "DESCENDING")),
            @Parameter(name = "sortBy", description = "정렬 기준", required = true,
                    schema = @Schema(type = "string", allowableValues = {"createdAt"}, defaultValue = "createdAt"))
    })
    ResponseEntity<CursorResponse<DirectMessageResponse>> getDirectMessages(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "대화 ID", required = true) @PathVariable UUID conversationId,
            @Parameter(hidden = true) DirectMessageCursorRequest request);

    @Operation(operationId = "markDirectMessagesAsRead", summary = "메시지 일괄 읽음 처리",
            description = "지정한 메시지 시점까지 받은 안 읽은 메시지를 일괄 읽음 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "대화 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화/메시지 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> markAsRead(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "대화 ID", required = true) @PathVariable UUID conversationId,
            @Parameter(description = "기준 메시지 ID", required = true) @PathVariable UUID directMessageId);
}
