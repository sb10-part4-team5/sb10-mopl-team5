package com.codeit.team5.mopl.auth.support;

import com.codeit.team5.mopl.auth.exception.OAuth2AuthorizationRequestCookieException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieValueSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DELIMITER = ".";

    private final AuthCookieProperties authCookieProperties;

    // 쿠키에 저장할 값에 HMAC 서명을 붙인다.
    public String sign(String value) {
        return value + DELIMITER + createSignature(value);
    }

    // 쿠키 서명을 검증하고, 검증에 성공하면 원본 값을 반환한다.
    public String verifyAndExtract(String signedValue) {
        int delimiterIndex = signedValue.lastIndexOf(DELIMITER);

        if (delimiterIndex <= 0 || delimiterIndex == signedValue.length() - 1) {
            throw new OAuth2AuthorizationRequestCookieException(
                    "OAuth2 authorization request 쿠키 형식이 올바르지 않습니다."
            );
        }

        String value = signedValue.substring(0, delimiterIndex);
        String signature = signedValue.substring(delimiterIndex + 1);
        String expectedSignature = createSignature(value);

        if (!MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new OAuth2AuthorizationRequestCookieException(
                    "OAuth2 authorization request 쿠키 서명이 올바르지 않습니다."
            );
        }

        return value;
    }

    private String createSignature(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    authCookieProperties.signatureSecretKey().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );

            mac.init(keySpec);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new OAuth2AuthorizationRequestCookieException(
                    "OAuth2 authorization request 쿠키 서명 생성에 실패했습니다.",
                    e
            );
        }
    }
}
