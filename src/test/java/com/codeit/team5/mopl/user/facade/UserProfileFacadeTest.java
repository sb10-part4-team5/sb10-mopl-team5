package com.codeit.team5.mopl.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentDeleteEvent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import com.codeit.team5.mopl.binarycontent.service.UploadWithRollback;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.exception.UserForbiddenException;
import com.codeit.team5.mopl.user.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserProfileFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private BinaryContentService binaryContentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserProfileFacade userProfileFacade;

    @BeforeEach
    void setUp() {
        userProfileFacade = new UserProfileFacade(
                userService, new UploadWithRollback(binaryContentService, eventPublisher));
    }

    private UserResponse response(UUID userId) {
        return new UserResponse(
                userId, Instant.parse("2026-06-25T00:00:00Z"),
                "user@example.com", "새이름", null, "USER", false);
    }

    @Test
    @DisplayName("본인이 아니면 업로드 전에 실패")
    void updateProfile_notOwner_throwsBeforeUpload() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");

        // when & then
        assertThatThrownBy(() -> userProfileFacade.updateProfile(
                currentUserId, userId, new UserUpdateRequest("새이름"), image))
                .isInstanceOf(UserForbiddenException.class);

        verifyNoInteractions(binaryContentService, userService, eventPublisher);
    }

    @Test
    @DisplayName("이미지가 있으면 스토리지 업로드 후 업데이트 성공")
    void updateProfile_withImage_success() {
        // given
        UUID userId = UUID.randomUUID();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        UploadedBinaryContent uploaded =
                new UploadedBinaryContent("profiles/key", "http://localhost/profiles/key.jpg");
        UserResponse expected = response(userId);
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.PROFILE), any()))
                .thenReturn(uploaded);
        when(userService.update(eq(userId), any(), eq(uploaded))).thenReturn(expected);

        // when
        UserResponse result = userProfileFacade.updateProfile(
                userId, userId, new UserUpdateRequest("새이름"), image);

        // then
        assertThat(result).isSameAs(expected);
        verify(binaryContentService).uploadToStorage(eq(StorageDirectory.PROFILE), any());
        verify(userService).update(eq(userId), any(), eq(uploaded));
        verify(eventPublisher, never()).publishEvent(any(BinaryContentDeleteEvent.class));
    }

    @Test
    @DisplayName("이미지가 없으면 업로드 없이 업데이트 성공")
    void updateProfile_noImage_success() {
        // given
        UUID userId = UUID.randomUUID();
        UserResponse expected = response(userId);
        when(userService.update(eq(userId), any(), isNull())).thenReturn(expected);

        // when
        UserResponse result = userProfileFacade.updateProfile(
                userId, userId, new UserUpdateRequest("새이름"), null);

        // then
        assertThat(result).isSameAs(expected);
        verify(userService).update(eq(userId), any(), isNull());
        verify(binaryContentService, never()).uploadToStorage(any(), any());
    }

    @Test
    @DisplayName("업로드 후 업데이트가 실패하면 삭제 이벤트 발행 후 예외 전파 성공")
    void updateProfile_updateFails_publishesDeleteEventAndPropagates() {
        // given
        UUID userId = UUID.randomUUID();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        UploadedBinaryContent uploaded =
                new UploadedBinaryContent("profiles/key", "http://localhost/profiles/key.jpg");
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.PROFILE), any()))
                .thenReturn(uploaded);
        when(userService.update(eq(userId), any(), eq(uploaded)))
                .thenThrow(new RuntimeException("db failure"));

        // when & then
        assertThatThrownBy(() -> userProfileFacade.updateProfile(
                userId, userId, new UserUpdateRequest("새이름"), image))
                .isInstanceOf(RuntimeException.class);

        verify(eventPublisher).publishEvent(new BinaryContentDeleteEvent(uploaded));
    }
}
