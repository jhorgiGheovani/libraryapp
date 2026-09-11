package com.jhorgi.libraryapp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asserts the generated document, not the configuration class.
 *
 * <p>The failure mode here is silent: a scheme that is declared but never
 * attached still boots, still renders, and simply has no Authorize button — you
 * find out by opening the page and wondering why every call is a 401. Reading
 * the spec back is the only check that catches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper json = new ObjectMapper();

    private JsonNode apiDocs;

    @BeforeEach
    void fetchSpec() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                // Also pins that the spec is public: it is in
                // SecurityConfig.PUBLIC_PATHS, and if that changes the docs become
                // unreachable for the people who need them.
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        apiDocs = json.readTree(body);
    }

    @Test
    void theBearerSchemeIsDeclared() {
        JsonNode scheme = apiDocs
                .path("components").path("securitySchemes").path(OpenApiConfig.BEARER_SCHEME);

        assertThat(scheme.isMissingNode()).isFalse();
        assertThat(scheme.path("type").asText()).isEqualTo("http");
        assertThat(scheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(scheme.path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    void theSchemeIsRequiredGloballySoProtectedEndpointsGetAPadlock() {
        JsonNode security = apiDocs.path("security");

        assertThat(security.isArray()).isTrue();
        assertThat(security.toString()).contains(OpenApiConfig.BEARER_SCHEME);
    }

    @Test
    void theAuthEndpointsOptOutBecauseTheyAreHowYouGetATokenInTheFirstPlace() {
        JsonNode paths = apiDocs.path("paths");

        for (String path : new String[]{"/auth/register", "/auth/login", "/auth/verify-otp"}) {
            JsonNode security = paths.path(path).path("post").path("security");

            // springdoc emits an empty array for @SecurityRequirements. That is the
            // explicit "no auth here", distinct from the node simply being absent.
            assertThat(security.isArray()).as("%s should declare its security", path).isTrue();
            assertThat(security).as("%s must not demand a token", path).isEmpty();
        }
    }

    @Test
    void theProtectedEndpointsAreDocumentedAndGrouped() {
        JsonNode paths = apiDocs.path("paths");

        assertThat(paths.has("/articles")).isTrue();
        assertThat(paths.has("/users")).isTrue();
        assertThat(paths.has("/audit-logs")).isTrue();
        assertThat(apiDocs.path("tags").toString()).contains("Audit log");
    }
}
