package com.codeit.team5.mopl.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaylistCreateRequest(@NotBlank String title,
                                    @NotBlank String description) {

}
