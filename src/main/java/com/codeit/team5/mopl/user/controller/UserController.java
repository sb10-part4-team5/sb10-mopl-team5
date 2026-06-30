package com.codeit.team5.mopl.user.controller;

import com.codeit.team5.mopl.binarycontent.support.MultipartFiles;
import com.codeit.team5.mopl.user.controller.api.UserApi;
import com.codeit.team5.mopl.user.dto.request.UserLockedUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserRoleUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController implements UserApi {
    private final UserService userService;

    @Override
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        log.info("User register request: POST /api/users");

        UserResponse response = userService.create(userRegisterRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.info("User detail request: GET /api/users/{}", userId);

        UserResponse response = userService.getById(userId);

        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestPart("request") UserUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("User update request: PATCH /api/users/{}", userId);

        UserResponse response = userService.update(userId, request, MultipartFiles.toImageResource(image));

        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping(value = "/{userId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        log.info("User role update request: PATCH /api/users/{}/role", userId);

        userService.updateRole(userId, request);

        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping(value = "/{userId}/locked")
    public ResponseEntity<Void> updateLockStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserLockedUpdateRequest request) {
        log.info("User lock status update request: PATCH /api/users/{}/locked", userId);

        userService.updateLock(userId, request);

        return ResponseEntity.noContent().build();
    }
}
