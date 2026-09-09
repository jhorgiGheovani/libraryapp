package com.jhorgi.libraryapp.adapter.in.web;

import com.jhorgi.libraryapp.adapter.in.web.dto.request.LoginRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.request.RegisterRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.request.VerifyOtpRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.ApiResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.MfaChallengeResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.TokenResponse;
import com.jhorgi.libraryapp.domain.model.MfaChallenge;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.LoginUseCase;
import com.jhorgi.libraryapp.domain.port.in.RegisterUseCase;
import com.jhorgi.libraryapp.domain.port.in.VerifyOtpUseCase;
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
    private final VerifyOtpUseCase verifyOtpUseCase;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase,
                          VerifyOtpUseCase verifyOtpUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResult>> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUseCase.register(
                request.fullname(), request.username(), request.email(), request.password());
        RegisterResult result =
                new RegisterResult(user.getId(), user.getFullname(), user.getUsername(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    /** Step 1 of 2: password check. Returns an OTP challenge, never a token. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MfaChallengeResponse>> login(@Valid @RequestBody LoginRequest request) {
        MfaChallenge challenge = loginUseCase.login(request.credential(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(MfaChallengeResponse.from(challenge)));
    }

    /** Step 2 of 2: the emailed code is exchanged for the JWT. */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String token = verifyOtpUseCase.verify(request.challengeId(), request.code());
        return ResponseEntity.ok(ApiResponse.ok(TokenResponse.bearer(token)));
    }

    public record RegisterResult(Long id, String fullname, String username, String email) {
    }
}
