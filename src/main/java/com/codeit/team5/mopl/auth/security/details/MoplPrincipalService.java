package com.codeit.team5.mopl.auth.security.details;

import com.codeit.team5.mopl.auth.entity.SocialAccount;
import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.mapper.AuthUserMapper;
import com.codeit.team5.mopl.auth.repository.SocialAccountRepository;
import com.codeit.team5.mopl.auth.security.details.oauth.OAuthUserInfo;
import com.codeit.team5.mopl.auth.support.EmailNormalizer;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MoplPrincipalService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthUserMapper authUserMapper;
    private final PasswordEncoder passwordEncoder;

    // PasswordAuthUser = form 로그인용 객체(AuthUser + password)
    // MoplUserDetails 에서 password 조합을 위해 또다시 userRepository 조회를 피하기 위함
    public PasswordAuthUser loadAuthUserWithPasswordByEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException(normalizedEmail));

        return authUserMapper.toAuthUserWithPassword(user);
    }

    public PasswordAuthUser loadAuthUserWithPasswordById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException(userId));

        return authUserMapper.toAuthUserWithPassword(user);
    }

    @Transactional
    public AuthUser getOrCreateAuthUser(OAuthUserInfo oauthUserInfo) {
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        oauthUserInfo.getProvider(),
                        oauthUserInfo.getProviderUserId()
                )
                .orElseGet(() -> createOrFindSocialAccount(oauthUserInfo));

        return authUserMapper.toAuthUser(socialAccount.getUser());
    }

    private SocialAccount createOrFindSocialAccount(OAuthUserInfo oauthUserInfo) {
        try {
            return createUserAndSocialAccount(oauthUserInfo);
        } catch (DataIntegrityViolationException e) {
            return socialAccountRepository
                    .findByProviderAndProviderUserId(
                            oauthUserInfo.getProvider(),
                            oauthUserInfo.getProviderUserId()
                    )
                    .orElseThrow(() -> e);
        }
    }

    private SocialAccount createUserAndSocialAccount(OAuthUserInfo oauthUserInfo) {
        User user = createSocialUser(oauthUserInfo);

        SocialAccount socialAccount = SocialAccount.create(
                user,
                oauthUserInfo.getProvider(),
                oauthUserInfo.getProviderUserId()
        );

        return socialAccountRepository.saveAndFlush(socialAccount);
    }

    private User createSocialUser(OAuthUserInfo oauthUserInfo) {
        String socialEmail = createSocialEmail(oauthUserInfo);

        User user = User.create(
                socialEmail,
                createRandomPassword(),
                oauthUserInfo.getName()
        );

        return userRepository.saveAndFlush(user);
    }

    private String createSocialEmail(OAuthUserInfo oauthUserInfo) {
        return oauthUserInfo.getProvider().name().toLowerCase()
                + "_"
                + oauthUserInfo.getProviderUserId()
                + "@oauth.mopl.local";
    }

    private String createRandomPassword() {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }
}
