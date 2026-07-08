package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.entity.SocialProvider;
import java.util.Map;

public class OAuthUserInfoFactory {

    public static OAuthUserInfo create(
            SocialProvider provider,
            Map<String, Object> attributes
    ) {
        return switch (provider) {
            case GOOGLE -> new GoogleOAuthUserInfo(attributes);
            case KAKAO -> new KakaoOAuthUserInfo(attributes);
        };
    }
}
