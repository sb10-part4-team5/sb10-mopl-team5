package com.codeit.team5.mopl.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.user.constant.UserSortBy;
import com.codeit.team5.mopl.user.dto.request.UserCursorRequest;
import com.codeit.team5.mopl.user.dto.request.UserLockedUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserRoleUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.exception.UserForbiddenException;
import com.codeit.team5.mopl.user.exception.SameLockStatusException;
import com.codeit.team5.mopl.user.exception.SameRoleAssignmentException;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BinaryContentService binaryContentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_success() {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "User@Example.COM", "password1");
        String normalizedEmail = "user@example.com";
        UserResponse expectedResponse = new UserResponse(
                UUID.randomUUID(),
                Instant.parse("2026-06-22T00:00:00Z"),
                normalizedEmail,
                request.name(),
                null,
                "USER",
                false
        );
        String encodedPassword = "encoded-password";

        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(userMapper.toDto(any(User.class))).thenReturn(expectedResponse);

        // When
        UserResponse result = userService.create(request);

        // Then
        assertThat(result).isSameAs(expectedResponse);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail(normalizedEmail);
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        verify(userMapper).toDto(savedUser);
        assertThat(savedUser.getEmail()).isEqualTo(normalizedEmail);
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getName()).isEqualTo(request.name());
        assertThat(savedUser.getRole().name()).isEqualTo("USER");
        assertThat(savedUser.isLocked()).isFalse();
    }

    @Test
    @DisplayName("중복 이메일 사용자 생성 실패")
    void createUser_duplicateEmail_throwsException() {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "duplicate@example.com", "password1");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicatedEmailException.class)
                .satisfies(exception -> assertThat(
                        ((DuplicatedEmailException) exception).getStatus()
                ).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getById_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        UserResponse expectedResponse = new UserResponse(
                userId,
                Instant.parse("2026-06-22T00:00:00Z"),
                "user@example.com",
                "사용자",
                null,
                "USER",
                false
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedResponse);

        // When
        UserResponse result = userService.getById(userId);

        // Then
        assertThat(result).isSameAs(expectedResponse);
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 실패")
    void getById_notFound_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("사용자 목록 조회 성공")
    void findUsers_success() {
        // Given
        UserCursorRequest request = new UserCursorRequest(
                "user",
                UserRole.USER,
                false,
                null,
                null,
                2,
                Sort.Direction.ASC,
                UserSortBy.EMAIL
        );
        User first = User.create("a-user@example.com", "encoded-password", "사용자A");
        User second = User.create("b-user@example.com", "encoded-password", "사용자B");
        User extra = User.create("c-user@example.com", "encoded-password", "사용자C");
        List<User> fetched = List.of(first, second, extra);
        CursorResponse<UserResponse> expectedResponse = new CursorResponse<>(
                List.of(
                        new UserResponse(UUID.randomUUID(), Instant.parse("2026-06-22T00:00:00Z"),
                                "a-user@example.com", "사용자A", null, "USER", false),
                        new UserResponse(UUID.randomUUID(), Instant.parse("2026-06-22T00:00:01Z"),
                                "b-user@example.com", "사용자B", null, "USER", false)
                ),
                "b-user@example.com",
                UUID.randomUUID().toString(),
                true,
                3,
                "email",
                "ASCENDING"
        );

        when(userRepository.findUsers(request, 3)).thenReturn(fetched);
        when(userRepository.countUsers(request)).thenReturn(3L);
        when(userMapper.toCursor(List.of(first, second), true, 3L, UserSortBy.EMAIL, Sort.Direction.ASC))
                .thenReturn(expectedResponse);

        // When
        CursorResponse<UserResponse> result = userService.findUsers(request);

        // Then
        assertThat(result).isSameAs(expectedResponse);
        verify(userRepository).findUsers(request, 3);
        verify(userRepository).countUsers(request);
        verify(userMapper).toCursor(List.of(first, second), true, 3L, UserSortBy.EMAIL, Sort.Direction.ASC);
    }

    @Test
    @DisplayName("프로필 이름만 변경 성공")
    void update_nameOnly_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "기존이름");
        UserResponse expected = new UserResponse(
                userId, Instant.parse("2026-06-25T00:00:00Z"),
                "user@example.com", "새이름", null, "USER", false
        );
        when(userRepository.findWithProfileImageById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponse result = userService.update(userId, userId, new UserUpdateRequest("새이름"), null);

        // Then
        assertThat(result).isSameAs(expected);
        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(user.getProfileImage()).isNull();
        verifyNoInteractions(binaryContentService, eventPublisher);
    }

    @Test
    @DisplayName("프로필 이미지 포함 변경 성공")
    void update_withImage_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "기존이름");
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        UserResponse expected = new UserResponse(
                userId, Instant.parse("2026-06-25T00:00:00Z"),
                "user@example.com", "새이름", "http://localhost/profiles/key.jpg", "USER", false
        );
        when(userRepository.findWithProfileImageById(userId)).thenReturn(Optional.of(user));
        when(binaryContentService.upload(eq(StorageDirectory.PROFILE), eq(user.getId()), any()))
                .thenReturn(BinaryContent.completed("http://localhost/profiles/key.jpg"));
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponse result = userService.update(userId, userId, new UserUpdateRequest("새이름"), image);

        // Then
        assertThat(result).isSameAs(expected);
        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(user.getProfileImage()).isNotNull();
        verify(binaryContentService).upload(eq(StorageDirectory.PROFILE), eq(user.getId()), any());
    }

    @Test
    @DisplayName("프로필 이미지 업로드 실패 시 변경 실패")
    void update_imageUploadFails_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "기존이름");
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        when(userRepository.findWithProfileImageById(userId)).thenReturn(Optional.of(user));
        when(binaryContentService.upload(eq(StorageDirectory.PROFILE), eq(user.getId()), any()))
                .thenThrow(new RuntimeException("S3 연결 실패"));

        // When & Then
        assertThatThrownBy(() -> userService.update(userId, userId, new UserUpdateRequest("새이름"), image))
                .isInstanceOf(RuntimeException.class);

        assertThat(user.getProfileImage()).isNull();
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("기존 프로필 이미지 교체 시 기존 이미지 DELETED 처리 성공")
    void update_replaceImage_oldImageDeleted() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "기존이름");
        BinaryContent oldImage = BinaryContent.completed("http://localhost/profiles/old.jpg");
        user.updateProfileImage(oldImage);
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        UserResponse expected = new UserResponse(
                userId, Instant.parse("2026-06-25T00:00:00Z"),
                "user@example.com", "새이름", "http://localhost/profiles/new.jpg", "USER", false
        );
        when(userRepository.findWithProfileImageById(userId)).thenReturn(Optional.of(user));
        when(binaryContentService.upload(eq(StorageDirectory.PROFILE), eq(user.getId()), any()))
                .thenReturn(BinaryContent.completed("http://localhost/profiles/new.jpg"));
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        userService.update(userId, userId, new UserUpdateRequest("새이름"), image);

        // Then
        assertThat(oldImage.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.DELETED);
        assertThat(user.getProfileImage().getUrl()).isEqualTo("http://localhost/profiles/new.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 프로필 변경 실패")
    void update_notFound_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findWithProfileImageById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.update(userId, userId, new UserUpdateRequest("새이름"), null))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findWithProfileImageById(userId);
        verifyNoInteractions(userMapper, binaryContentService, eventPublisher);
    }

    @Test
    @DisplayName("본인이 아닌 사용자의 프로필 변경 실패")
    void update_notOwner_throwsException() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> userService.update(currentUserId, userId, new UserUpdateRequest("새이름"), null))
                .isInstanceOf(UserForbiddenException.class);

        verifyNoInteractions(userRepository, userMapper, binaryContentService, eventPublisher);
    }

    @Test
    @DisplayName("사용자 권한 변경 성공")
    void updateRole_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.updateRole(userId, new UserRoleUpdateRequest(UserRole.ADMIN));

        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findById(userId);
        verify(refreshTokenStore).deleteByUserId(userId);

        ArgumentCaptor<RoleChangedEvent> eventCaptor = ArgumentCaptor.forClass(RoleChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        RoleChangedEvent event = eventCaptor.getValue();
        assertThat(event.receiverId()).isEqualTo(userId);
        assertThat(event.roleBefore()).isEqualTo("USER");
        assertThat(event.roleAfter()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 권한 변경 실패")
    void updateRole_notFound_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateRole(userId, new UserRoleUpdateRequest(UserRole.ADMIN)))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(refreshTokenStore, eventPublisher);
    }

    @Test
    @DisplayName("동일한 권한으로 변경하면 실패하고 후속 처리를 하지 않는다")
    void updateRole_sameRole_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateRole(userId, new UserRoleUpdateRequest(UserRole.USER)))
                .isInstanceOf(SameRoleAssignmentException.class);

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).findById(userId);
        verifyNoInteractions(refreshTokenStore, eventPublisher);
    }

    @Test
    @DisplayName("사용자 계정 잠금 성공")
    void updateLock_lockedTrue_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.updateLock(userId, new UserLockedUpdateRequest(true));

        // Then
        assertThat(user.isLocked()).isTrue();
        verify(userRepository).findById(userId);
        verify(refreshTokenStore).deleteByUserId(userId);
    }

    @Test
    @DisplayName("사용자 계정 잠금 해제 성공")
    void updateLock_lockedFalse_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        user.updateLocked(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.updateLock(userId, new UserLockedUpdateRequest(false));

        // Then
        assertThat(user.isLocked()).isFalse();
        verify(userRepository).findById(userId);
        verify(refreshTokenStore).deleteByUserId(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 계정 잠금 상태 변경 실패")
    void updateLock_notFound_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateLock(userId, new UserLockedUpdateRequest(true)))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(refreshTokenStore);
    }

    @Test
    @DisplayName("동일한 잠금 상태로 변경하면 실패하고 후속 처리를 하지 않는다")
    void updateLock_sameStatus_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateLock(userId, new UserLockedUpdateRequest(false)))
                .isInstanceOf(SameLockStatusException.class);

        assertThat(user.isLocked()).isFalse();
        verify(userRepository).findById(userId);
        verifyNoInteractions(refreshTokenStore);
    }
}
