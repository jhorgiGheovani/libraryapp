package com.jhorgi.libraryapp.adapter.in.web.dto.request;

import com.jhorgi.libraryapp.domain.model.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateArticleRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 10_000)
        String content,

        Visibility visibility
) {
}
