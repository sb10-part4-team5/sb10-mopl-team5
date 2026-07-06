package com.codeit.team5.mopl.auth.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_accounts_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    // 추후에 일반 로그인 사용자가 소셜 계정과 연동시를 대비
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_social_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    public static SocialAccount create(User user, SocialProvider provider, String providerUserId) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.user = user;
        socialAccount.provider = provider;
        socialAccount.providerUserId = providerUserId;

        return socialAccount;
    }
}
