package com.jhorgi.libraryapp.adapter.in.web.dto.request;

import com.jhorgi.libraryapp.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(

        @NotNull
        Role role
) {
}
