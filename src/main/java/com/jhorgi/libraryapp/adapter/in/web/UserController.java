package com.jhorgi.libraryapp.adapter.in.web;

import com.jhorgi.libraryapp.adapter.in.web.dto.request.ChangeRoleRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.request.CreateUserRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.request.UpdateUserRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.ApiResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.PageResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.UserResponse;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.UserManagementUseCase;
import com.jhorgi.libraryapp.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account administration, SUPER_ADMIN only. The gate is on the class: every
 * method needs USER_MANAGE, so a new endpoint added here cannot be left open by
 * forgetting the annotation.
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserController {

    private final UserManagementUseCase users;

    public UserController(UserManagementUseCase users) {
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        User created = users.create(new UserManagementUseCase.CreateUserCommand(
                request.fullname(), request.username(), request.email(),
                request.password(), request.role(), caller.actor()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserResponse.from(created)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        PagedResult<User> result = users.list(caller.actor(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result, UserResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(users.getById(id, caller.actor()))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        User updated = users.updateProfile(new UserManagementUseCase.UpdateUserCommand(
                id, request.fullname(), request.username(), request.email(),
                request.password(), caller.actor()));
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(updated)));
    }

    /** Role changes get their own endpoint so the privilege move is explicit. */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        User updated = users.changeRole(id, request.role(), caller.actor());
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(updated)));
    }

    /** Deletes the account and every article it authored. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        users.delete(id, caller.actor());
        return ResponseEntity.noContent().build();
    }
}
