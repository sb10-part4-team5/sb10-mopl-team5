package com.codeit.team5.mopl.auth.support;

import com.codeit.team5.mopl.auth.exception.OAuth2AuthorizationRequestCookieException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "OAUTH2_AUTHORIZATION_REQUEST";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";

    // OAuth 인가 요청은 짧은 시간 안에 완료되므로 5분만 유지
    private static final int COOKIE_EXPIRE_SECONDS = 300;

    private final CookieValueSigner cookieValueSigner;

    // 쿠키에서 OAuth2 AuthorizationRequest를 조회
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);

        if (cookie == null) {
            return null;
        }

        try {
            String serializedValue = cookieValueSigner.verifyAndExtract(cookie.getValue());
            return deserialize(serializedValue);
        } catch (RuntimeException e) {
            log.warn("Invalid OAuth2 authorization request cookie.", e);
            return null;
        }
    }

    // OAuth2 AuthorizationRequest를 직렬화하고 서명한 뒤 쿠키에 저장
    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        String serializedValue = serialize(authorizationRequest);
        String signedValue = cookieValueSigner.sign(serializedValue);

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, signedValue)
                .httpOnly(true)
                .secure(request.isSecure())
                .path(COOKIE_PATH)
                .maxAge(COOKIE_EXPIRE_SECONDS)
                .sameSite(SAME_SITE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 저장된 AuthorizationRequest를 반환한 뒤 쿠키를 제거
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authorizationRequest;
    }

    // AuthorizationRequest 쿠키를 만료시켜 제거
    public void removeAuthorizationRequestCookies(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite(SAME_SITE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream =
                        new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(authorizationRequest);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new OAuth2AuthorizationRequestCookieException(
                    "OAuth2 authorization request 직렬화에 실패했습니다.",
                    e
            );
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        byte[] bytes = Base64.getUrlDecoder().decode(value);

        try (ObjectInputStream objectInputStream =
                new ObjectInputStream(new ByteArrayInputStream(bytes))) {

            objectInputStream.setObjectInputFilter(OAUTH2_AUTHORIZATION_REQUEST_FILTER);

            return (OAuth2AuthorizationRequest) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new OAuth2AuthorizationRequestCookieException(
                    "OAuth2 authorization request 역직렬화에 실패했습니다.",
                    e
            );
        }
    }

    // OAuth2AuthorizationRequest 역직렬화 시 허용할 클래스만 제한한다.
    // Java 기본 역직렬화(ObjectInputStream)는 임의 객체를 생성할 수 있으므로,
    // Spring Security OAuth2 관련 타입과 JDK 기본 타입만 허용하고 나머지는 모두 차단한다.
    // 또한 역직렬화 깊이, 참조 수, 데이터 크기를 제한해 과도한 객체 생성 공격도 방지한다.
    private static final ObjectInputFilter OAUTH2_AUTHORIZATION_REQUEST_FILTER =
            ObjectInputFilter.Config.createFilter(
                    "maxdepth=20;"
                            + "maxrefs=200;"
                            + "maxbytes=65536;"
                            + "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;"
                            + "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest$*;"
                            + "org.springframework.security.oauth2.core.endpoint.*;"
                            + "org.springframework.security.oauth2.core.*;"
                            + "java.base/*;"
                            + "java.util.*;"
                            + "!*"
            );
}
