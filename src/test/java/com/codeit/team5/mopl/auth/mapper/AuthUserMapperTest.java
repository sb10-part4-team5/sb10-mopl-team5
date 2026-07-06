package com.codeit.team5.mopl.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthUserMapperTest {

    private final AuthUserMapper authUserMapper = new AuthUserMapperImpl();

    @Test
    @DisplayName("User를 AuthUser로 매핑 성공")
    void toAuthUser_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        user.updateRole(UserRole.ADMIN);
        user.updateLocked(true);

        // when
        AuthUser authUser = authUserMapper.toAuthUser(user);

        // then
        assertThat(authUser.id()).isEqualTo(userId);
        assertThat(authUser.email()).isEqualTo("user@example.com");
        assertThat(authUser.role()).isEqualTo("ADMIN");
        assertThat(authUser.locked()).isTrue();
    }

    @Test
    @DisplayName("AuthUser 매핑 시 프로필 이미지 LAZY 연관에 접근하지 않음 성공")
    void toAuthUser_doesNotAccessProfileImage_success() {
        // given
        User user = spy(User.create("user@example.com", "encoded", "사용자"));

        // when
        authUserMapper.toAuthUser(user);

        // then
        verify(user, never()).getProfileImage();
    }
}
