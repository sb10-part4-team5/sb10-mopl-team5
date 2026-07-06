package com.codeit.team5.mopl.follow.controller;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.follow.controller.api.FollowApi;
import com.codeit.team5.mopl.follow.dto.request.FollowCreateRequest;
import com.codeit.team5.mopl.follow.dto.response.FollowResponse;
import com.codeit.team5.mopl.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@Slf4j
public class FollowController implements FollowApi {

    private final FollowService followService;

    @Override
    @PostMapping
    public ResponseEntity<FollowResponse> follow(
            @AuthenticationPrincipal MoplPrincipal principal,
            @Valid @RequestBody FollowCreateRequest request) {
        log.info("Follow request: POST /api/follows, followee={}", request.followeeId());

        FollowResponse response = followService.follow(principal.getId(), request.followeeId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/followed-by-me")
    public ResponseEntity<FollowResponse> getFollowedByMe(
            @AuthenticationPrincipal MoplPrincipal principal,
            @RequestParam UUID followeeId) {
        log.info("Follow check request: GET /api/follows/followed-by-me, followee={}", followeeId);

        FollowResponse response = followService.getFollowedByMe(principal.getId(), followeeId);

        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/count")
    public ResponseEntity<Long> countFollowers(@RequestParam UUID followeeId) {
        log.info("Follower count request: GET /api/follows/count, followee={}", followeeId);

        return ResponseEntity.ok(followService.countFollowers(followeeId));
    }

    @Override
    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal MoplPrincipal principal,
            @PathVariable UUID followId) {
        log.info("Unfollow request: DELETE /api/follows/{}", followId);

        followService.unfollow(principal.getId(), followId);

        return ResponseEntity.noContent().build();
    }
}
