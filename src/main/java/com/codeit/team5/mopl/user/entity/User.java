package com.codeit.team5.mopl.user.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.global.exception.ErrorCode;
import com.codeit.team5.mopl.user.exception.InvalidPasswordException;
import com.codeit.team5.mopl.user.exception.InvalidUsernameException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean locked;

    public static User create(
            String email,
            String password,
            String name
    ) {
        User user = new User();
        user.email = email;
        user.name = name;
        user.password = password;
        user.locked = false;
        user.role = UserRole.USER;
        user.profileImageUrl = null;

        return user;
    }

    public void updateProfile(
            String newName,
            String profileImageUrl
    ) {
        if (newName == null || newName.isBlank()) {
            throw new InvalidUsernameException();
        }
        this.name = newName;
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidPasswordException();
        }
        this.password = newPassword;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void updateLocked(boolean locked) {
        this.locked = locked;
    }
}