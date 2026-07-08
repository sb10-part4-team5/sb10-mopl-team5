package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.entity.SocialProvider;

public interface OAuthUserInfo {

    SocialProvider getProvider();

    String getProviderUserId();

    String getEmail();

    String getName();
}
