package com.codeit.team5.mopl.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.entity.User;
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
class UserServiceImageRollbackIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private BinaryContentStorage binaryContentStorage;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("프로필 이미지 업로드 실패 시 트랜잭션 롤백으로 기존 정보 유지 성공")
    void update_imageUploadFails_rollsBack() {
        // given
        User saved = userRepository.save(User.create("rollback@example.com", "encoded-password", "기존이름"));
        UUID userId = saved.getId();
        doThrow(new RuntimeException("S3 업로드 실패"))
                .when(binaryContentStorage).store(any(), any(), any());
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");

        // when & then
        assertThatThrownBy(() ->
                userService.update(userId, userId, new UserUpdateRequest("새이름"), image))
                .isInstanceOf(RuntimeException.class);

        // 트랜잭션 롤백으로 이름 변경과 프로필 이미지가 모두 반영되지 않아야 한다
        User reloaded = userRepository.findWithProfileImageById(userId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("기존이름");
        assertThat(reloaded.getProfileImage()).isNull();
    }
}
