package com.jhorgi.libraryapp.adapter.in.web;

import com.jhorgi.libraryapp.adapter.in.web.dto.request.CreateArticleRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.request.UpdateArticleRequest;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.ApiResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.ArticleResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.PageResponse;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase;
import com.jhorgi.libraryapp.domain.port.in.ArticleQueryUseCase;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/articles")
public class ArticleController {
    private final ArticleCommandUseCase commands;
    private final ArticleQueryUseCase queries;

    public ArticleController(ArticleCommandUseCase commands, ArticleQueryUseCase queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ARTICLE_CREATE')")
    public ResponseEntity<ApiResponse<ArticleResponse>> create(
            @Valid @RequestBody CreateArticleRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        // The author is taken from the token, never from the payload.
        Article created = commands.create(new ArticleCommandUseCase.CreateArticleCommand(
                request.title(), request.content(), request.visibility(), caller.actor()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ArticleResponse.from(created)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        PagedResult<Article> result = queries.list(caller.actor(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result, ArticleResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        Article article = queries.getById(id, caller.actor());
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(article)));
    }

    // The role gate is declarative; ownership needs the loaded article and stays
    // in ArticlePolicy. A role-only denial leaks nothing about the id, so
    // answering 403 here rather than 404 is not an existence oracle.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ARTICLE_UPDATE_OWN', 'ARTICLE_WRITE_ANY')")
    public ResponseEntity<ApiResponse<ArticleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateArticleRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        Article updated = commands.update(new ArticleCommandUseCase.UpdateArticleCommand(
                id, request.title(), request.content(), request.visibility(), caller.actor()));
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ARTICLE_DELETE_OWN', 'ARTICLE_WRITE_ANY')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        commands.delete(id, caller.actor());
        return ResponseEntity.noContent().build();
    }
}
