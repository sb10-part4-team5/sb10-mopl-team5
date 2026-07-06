package com.codeit.team5.mopl.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserProfileFacadeIntegrationTest {

    @Autowired
    private UserProfileFacade userProfileFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BinaryContentRepository binaryContentRepository;

    @MockitoBean
    private BinaryContentStorage binaryContentStorage;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        binaryContentRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 흐름에서 스토리지 저장과 프로필 이미지 연결 성공")
    void updateProfile_success_persistsImage() {
        // given
        User saved = userRepository.save(User.create("owner@example.com", "encoded-password", "기존이름"));
        UUID userId = saved.getId();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        when(binaryContentStorage.toUrl(any())).thenReturn("http://localhost/profiles/key.jpg");

        // when
        userProfileFacade.updateProfile(userId, userId, new UserUpdateRequest("새이름"), image);

        // then
        User reloaded = userRepository.findWithProfileImageById(userId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("새이름");
        assertThat(reloaded.getProfileImage()).isNotNull();
        verify(binaryContentStorage).store(any(), any(), any());
        verify(binaryContentStorage, never()).delete(any());
    }

    @Test
    @DisplayName("업데이트 실패 시 스토리지 보상 삭제 후 DB 저장 없음 성공")
    void updateProfile_updateFails_compensatesAndNoDbWrite() {
        // given
        UUID missingUserId = UUID.randomUUID();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        when(binaryContentStorage.toUrl(any())).thenReturn("http://localhost/profiles/key.jpg");

        // when & then
        assertThatThrownBy(() -> userProfileFacade.updateProfile(
                missingUserId, missingUserId, new UserUpdateRequest("새이름"), image))
                .isInstanceOf(UserNotFoundException.class);

        // 업로드한 스토리지 객체는 비동기로 보상 삭제되고, BinaryContent는 저장되지 않아야 한다
        verify(binaryContentStorage).store(any(), any(), any());
        verify(binaryContentStorage, timeout(2000)).delete(any());
        assertThat(binaryContentRepository.count()).isZero();
    }
}
