package com.codeit.team5.mopl.watcher.controller.api;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "시청 세션 관리")
public interface WatchingSessionRestApi {

    @Operation(summary = "특정 사용자의 시청 세션 조회 (nullable)", operationId = "findWatchingSessionsByWatcher")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    ResponseEntity<WatchingSessionResponse> findWatchingSessionsByWatcher(
            @Parameter(name = "watcherId", description = "시청자 ID", required = true) UUID watcherId
    );

    @Operation(summary = "특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)", operationId = "findWatchingSessionsByContent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    ResponseEntity<CursorResponse<WatchingSessionResponse>> findWatchingSessionsByContent(
            @Parameter(name = "contentId", description = "콘텐츠 ID", required = true) UUID contentId,
            @ParameterObject WatchingSessionCursorRequest request
    );
}
