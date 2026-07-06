package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.entity.SocialProvider;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipalService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

// OAuth 객체 생성용 클래스
@Service
@RequiredArgsConstructor
public class MoplOAuth2UserService extends DefaultOAuth2UserService {

    private final MoplPrincipalService moplPrincipalService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 여기서 provider + providerUserId 추출
        SocialProvider provider = SocialProvider.from(registrationId);

        // OAuth 제공자가 내려주는 사용자 정보 전체(Map)
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthUserInfo oauthUserInfo =
                OAuthUserInfoFactory.create(provider, attributes);

        AuthUser authUser =
                moplPrincipalService.getOrCreateAuthUser(oauthUserInfo);

        if (authUser.locked()) {
            // 실패 핸들러로 넘기기 위해 spring security exception 사용
            throw new LockedException("잠긴 계정입니다.");
        }

        return new MoplOAuth2User(authUser, attributes, nameAttributeKey);
    }
}
