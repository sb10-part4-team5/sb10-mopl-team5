package com.codeit.team5.mopl.user.entity;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.user.exception.InvalidPasswordException;
import com.codeit.team5.mopl.user.exception.InvalidUsernameException;
import com.codeit.team5.mopl.user.exception.SameLockStatusException;
import com.codeit.team5.mopl.user.exception.SameRoleAssignmentException;
import jakarta.persistence.*;
import java.util.Objects;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id", unique = true)
    private BinaryContent profileImage;

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
        return user;
    }

    public void updateProfile(String newName, BinaryContent profileImage) {
        if (newName == null || newName.isBlank()) {
            throw new InvalidUsernameException();
        }
        this.name = newName;
        this.profileImage = profileImage;
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidPasswordException();
        }
        this.password = newPassword;
    }

    public void updateRole(UserRole role) {
        Objects.requireNonNull(role, "role must not be null");
        if (role == this.role) {
            throw new SameRoleAssignmentException(this.role.toString());
        }
        this.role = role;
    }

    public void updateLocked(boolean locked) {
        if (locked == this.locked) {
            throw new SameLockStatusException(this.locked);
        }
        this.locked = locked;
    }
}
