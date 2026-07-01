package com.codeit.team5.mopl.auth.generator;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordGenerator {

    private static final String CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return sb.toString();
    }
}
