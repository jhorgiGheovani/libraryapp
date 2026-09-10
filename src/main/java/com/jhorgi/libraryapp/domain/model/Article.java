package com.jhorgi.libraryapp.domain.model;

import java.time.Instant;

public record Article(
        Long id,
        String title,
        String content,
        Long authorId,
        Visibility visibility,
        Instant createdAt,
        Instant updatedAt
) {

    public static Article newArticle(String title, String content, Long authorId, Visibility visibility) {
        return new Article(null, title, content, authorId, visibility, null, null);
    }


    public Article withRevision(String title, String content, Visibility visibility) {
        return new Article(id, title, content, authorId, visibility, createdAt, updatedAt);
    }

    public boolean isOwnedBy(Long userId) {
        return authorId != null && authorId.equals(userId);
    }

    public boolean isPublic() {
        return visibility == Visibility.PUBLIC;
    }


    public boolean isVisibleTo(Long userId) {
        return isPublic() || isOwnedBy(userId);
    }
}
