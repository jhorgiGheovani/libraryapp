package com.jhorgi.libraryapp.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the JWT bearer scheme so Swagger UI has an <em>Authorize</em> button.
 *
 * <p>Without it the UI renders and every protected endpoint returns 401 from it,
 * because there is nowhere to put a token — which makes the documentation
 * unusable for exactly the endpoints that need documenting most.
 *
 * <p>The requirement is declared globally rather than per-controller: the filter
 * chain's rule is {@code anyRequest().authenticated()}, so "needs a token" is
 * the default and the exceptions are the short list. {@code AuthController}
 * opts out with {@code @SecurityRequirements}, matching {@code PUBLIC_PATHS} in
 * {@code SecurityConfig}.
 */
@Configuration
@SecurityScheme(
        name = OpenApiConfig.BEARER_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the token from POST /auth/verify-otp. The \"Bearer \" prefix is added for you."
)
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI libraryappOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library App API")
                        .version("v1")
                        .description("""
                                Authentication is two-step: POST /auth/login returns an OTP challenge,
                                POST /auth/verify-otp exchanges the emailed code for the JWT. No token
                                is issued before the code is verified, so the second factor is not
                                decorative.

                                Every other endpoint needs that token. Roles: SUPER_ADMIN, EDITOR,
                                CONTRIBUTOR, VIEWER."""))
                // Components is set explicitly so the scheme exists even if the
                // annotation scan finds nothing to attach it to.
                .components(new Components())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
