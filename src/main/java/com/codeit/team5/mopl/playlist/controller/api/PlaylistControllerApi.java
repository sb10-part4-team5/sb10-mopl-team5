package com.codeit.team5.mopl.playlist.controller.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCursorRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "플레이리스트 관리")
public interface PlaylistControllerApi {

    @Operation(summary = "플레이리스트 단건 조회")
    ResponseEntity<PlaylistResponse> find(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID id);

    @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)")
    ResponseEntity<CursorResponse<PlaylistResponse>> findCursor(
            @Parameter(hidden = true) MoplPrincipal principal,
            @ModelAttribute PlaylistCursorRequest request);

    @Operation(summary = "플레이리스트 생성", description = "생성한 플레이리스트는 API 요청자 본인의 플레이리스트로 생성됩니다.")
    ResponseEntity<PlaylistResponse> create(@Parameter(hidden = true) MoplPrincipal principal,
            @RequestBody PlaylistCreateRequest request);

    @Operation(summary = "플레이리스트 수정", description = "플레이리스트 소유자만 수정할 수 있습니다.")
    ResponseEntity<PlaylistResponse> update(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID id, @RequestBody PlaylistUpdateRequest request);

    @Operation(summary = "플레이리스트 삭제", description = "플레이리스트 소유자만 삭제할 수 있습니다.")
    ResponseEntity<Void> delete(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID playlistId);

    @Operation(summary = "플레이리스트에 콘텐츠 추가", description = "플레이리스트 소유자만 콘텐츠를 추가할 수 있습니다.")
    ResponseEntity<Void> addContent(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID playlistId, @PathVariable UUID contentId);

    @Operation(summary = "플레이리스트에서 콘텐츠 삭제", description = "플레이리스트 소유자만 콘텐츠를 삭제할 수 있습니다.")
    ResponseEntity<Void> removeContent(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID playlistId, @PathVariable UUID contentId);
}
