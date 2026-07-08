package com.codeit.team5.mopl.follow.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.follow.dto.request.FollowCreateRequest;
import com.codeit.team5.mopl.follow.dto.response.FollowResponse;
import com.codeit.team5.mopl.global.dto.ErrorResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "팔로우 관리", description = "팔로우 관련 API")
public interface FollowApi {

    @Operation(operationId = "createFollow", summary = "팔로우")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "팔로우 성공",
                    content = @Content(schema = @Schema(implementation = FollowResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(자기 자신 팔로우 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대상 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 팔로우한 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FollowResponse> follow(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "팔로우 요청 본문", required = true)
            @Valid @RequestBody FollowCreateRequest request);

    @Operation(operationId = "isFollowedByMe", summary = "특정 유저를 내가 팔로우하는지 여부 조회",
            description = "팔로우 중이면 FollowDto(id 포함)를 반환합니다. 팔로우하지 않은 경우 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 중",
                    content = @Content(schema = @Schema(implementation = FollowResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "팔로우하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FollowResponse> getFollowedByMe(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "대상 사용자 ID", required = true)
            @RequestParam UUID followeeId);

    @Operation(operationId = "getFollowerCount", summary = "특정 유저의 팔로워 수 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Long> countFollowers(
            @Parameter(description = "대상 사용자 ID", required = true)
            @RequestParam UUID followeeId);

    @Operation(operationId = "cancelFollow", summary = "팔로우 취소",
            description = "API 요청자 본인의 팔로우만 취소할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "팔로우 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류(본인 팔로우만 취소 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "팔로우 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> unfollow(
            @Parameter(hidden = true) MoplPrincipal principal,
            @Parameter(description = "팔로우 ID", required = true) UUID followId);
}
