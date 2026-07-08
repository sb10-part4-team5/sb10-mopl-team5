package com.codeit.team5.mopl.dm.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;

@Tag(name = "DM 대화 관리", description = "다이렉트 메시지 대화 API")
public interface ConversationApi {

    @Operation(operationId = "createConversation", summary = "대화 생성",
            description = "대상 사용자와의 대화를 생성합니다. 이미 존재하면 기존 대화를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 생성/조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(자기 자신과 대화 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대상 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ConversationResponse> createConversation(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "대화 생성 요청 본문", required = true)
            @Valid @RequestBody ConversationCreateRequest request);

    @Operation(operationId = "getMyConversations", summary = "내 대화 목록 조회",
            description = "내가 참여한 대화 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Parameters({
            @Parameter(name = "keywordLike", description = "상대방 이름 검색 키워드"),
            @Parameter(name = "cursor", description = "커서"),
            @Parameter(name = "idAfter", description = "보조 커서"),
            @Parameter(name = "limit", description = "한 번에 가져올 개수", example = "20", required = true),
            @Parameter(name = "sortDirection", description = "정렬 방향", required = true,
                    schema = @Schema(type = "string", allowableValues = {"ASCENDING", "DESCENDING"},
                            defaultValue = "DESCENDING")),
            @Parameter(name = "sortBy", description = "정렬 기준", required = true,
                    schema = @Schema(type = "string", allowableValues = {"createdAt"}, defaultValue = "createdAt"))
    })
    ResponseEntity<CursorResponse<ConversationResponse>> getMyConversations(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(hidden = true) ConversationCursorRequest request);

    @Operation(operationId = "getConversationWith", summary = "특정 사용자와의 대화 조회",
            description = "상대 사용자와의 대화를 조회합니다. 없으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ConversationResponse> getConversationWith(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "상대 사용자 ID", required = true) @RequestParam UUID userId);

    @Operation(operationId = "getConversation", summary = "대화 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "대화 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ConversationResponse> getConversation(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "대화 ID", required = true) @PathVariable UUID conversationId);
}
