package com.codeit.team5.mopl.playlist.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record PlaylistCreateRequest(@NotEmpty String title,
                                    @NotEmpty String description) {

}
