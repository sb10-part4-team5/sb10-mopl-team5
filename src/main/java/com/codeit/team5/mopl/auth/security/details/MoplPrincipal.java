package com.codeit.team5.mopl.auth.security.details;

import java.util.UUID;

public interface MoplPrincipal {

    UUID getId();

    String getEmail();

    String getRole();

    boolean isLocked();
}
