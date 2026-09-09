package com.jhorgi.libraryapp.adapter.in.web;

import com.jhorgi.libraryapp.adapter.in.web.dto.request.LoginRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.request.RegisterRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.ApiResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.TokenResponse;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.LoginUseCase;
import com.jhorgi.libraryapp.domain.port.in.RegisterUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResult>> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUseCase.register(request.username(), request.email(), request.password());
        RegisterResult result = new RegisterResult(user.getId(), user.getUsername(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(TokenResponse.bearer(token)));
    }

    public record RegisterResult(Long id, String username, String email) {
    }
}
