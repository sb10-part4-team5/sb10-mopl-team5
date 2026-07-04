package com.codeit.team5.mopl.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(@NotBlank @Size(max = 255) String title,
                                    @NotBlank String description) {

}
