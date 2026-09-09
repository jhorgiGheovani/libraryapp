package com.jhorgi.libraryapp.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(

        @NotBlank
        String challengeId,

        @NotBlank
        @Pattern(regexp = "\\d{4,8}", message = "must be a numeric code")
        String code
) {
}
