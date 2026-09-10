package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.ArticleEntity;
import com.jhorgi.libraryapp.domain.model.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the derived visibility query the fake cannot prove: that the filter runs
 * in SQL, so LIMIT and COUNT both respect it. Embedded H2 via @DataJpaTest, no
 * Docker.
 */
@DataJpaTest
class ArticleJpaRepositoryTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    @Autowired
    private ArticleJpaRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        save("Alice public", ALICE, Visibility.PUBLIC);
        save("Alice draft", ALICE, Visibility.PRIVATE);
        save("Bob public", BOB, Visibility.PUBLIC);
        save("Bob draft", BOB, Visibility.PRIVATE);
    }

    private ArticleEntity save(String title, Long authorId, Visibility visibility) {
        return repository.save(new ArticleEntity(null, title, "Content", authorId, visibility, null, null));
    }

    @Test
    void timestampsArePopulatedOnInsert() {
        ArticleEntity saved = save("Fresh", ALICE, Visibility.PUBLIC);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void viewerSeesPublicArticlesPlusTheirOwnDrafts() {
        Page<ArticleEntity> page = repository.findByVisibilityOrAuthorId(
                Visibility.PUBLIC, BOB, PageRequest.of(0, 10, NEWEST_FIRST));

        List<String> titles = page.getContent().stream().map(ArticleEntity::getTitle).toList();
        assertThat(titles).containsExactlyInAnyOrder("Alice public", "Bob public", "Bob draft");
    }

    @Test
    void totalCountIgnoresHiddenArticles() {
        // The count must come from the same WHERE clause as the rows, or it
        // reveals how many drafts other people have.
        Page<ArticleEntity> page = repository.findByVisibilityOrAuthorId(
                Visibility.PUBLIC, BOB, PageRequest.of(0, 10, NEWEST_FIRST));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void pagingIsAppliedInSqlNotAfterFiltering() {
        Page<ArticleEntity> first = repository.findByVisibilityOrAuthorId(
                Visibility.PUBLIC, BOB, PageRequest.of(0, 2, NEWEST_FIRST));
        Page<ArticleEntity> second = repository.findByVisibilityOrAuthorId(
                Visibility.PUBLIC, BOB, PageRequest.of(1, 2, NEWEST_FIRST));

        // A full first page is the tell: filtering after the fact would return
        // fewer rows than requested even though more exist.
        assertThat(first.getContent()).hasSize(2);
        assertThat(second.getContent()).hasSize(1);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).doesNotContainAnyElementsOf(second.getContent());
    }

    @Test
    void authorSeesOnlyTheirOwnDraftNotOthers() {
        Page<ArticleEntity> page = repository.findByVisibilityOrAuthorId(
                Visibility.PUBLIC, ALICE, PageRequest.of(0, 10, NEWEST_FIRST));

        assertThat(page.getContent()).extracting(ArticleEntity::getTitle)
                .contains("Alice draft")
                .doesNotContain("Bob draft");
    }
}
