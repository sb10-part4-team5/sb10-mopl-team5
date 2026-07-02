package com.codeit.team5.mopl.auth.event;

public record TemporaryPasswordIssuedEvent(
        String email,
        String temporaryPassword
) {

}
