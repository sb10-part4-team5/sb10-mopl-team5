package com.codeit.team5.mopl.dm.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "DM 관리", description = "다이렉트 메시지/대화 관련 API")
public interface ConversationApi {

    @Operation(operationId = "createConversation", summary = "대화 생성",
            description = "대상 사용자와의 대화를 생성합니다. 이미 존재하면 기존 대화를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 생성/조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(자기 자신과 대화 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "대상 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<ConversationResponse> createConversation(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "대화 생성 요청 본문", required = true)
            @Valid @RequestBody ConversationCreateRequest request);

    @Operation(operationId = "getConversationWith", summary = "특정 사용자와의 대화 조회",
            description = "상대 사용자와의 대화를 조회합니다. 없으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<ConversationResponse> getConversationWith(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "상대 사용자 ID", required = true) @RequestParam UUID userId);

    @Operation(operationId = "getConversation", summary = "대화 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "대화 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<ConversationResponse> getConversation(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "대화 ID", required = true) @PathVariable UUID conversationId);

    @Operation(operationId = "markDirectMessagesAsRead", summary = "메시지 일괄 읽음 처리",
            description = "지정한 메시지 시점까지 받은 안 읽은 메시지를 일괄 읽음 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "대화 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "대화/메시지 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> markAsRead(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "대화 ID", required = true) @PathVariable UUID conversationId,
            @Parameter(description = "기준 메시지 ID", required = true) @PathVariable UUID directMessageId);
}
