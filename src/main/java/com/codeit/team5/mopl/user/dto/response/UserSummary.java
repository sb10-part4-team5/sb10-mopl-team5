package com.codeit.team5.mopl.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record UserSummary(@JsonProperty("userId")UUID id,
                          String name,
                          String profileImageUrl) {

}
