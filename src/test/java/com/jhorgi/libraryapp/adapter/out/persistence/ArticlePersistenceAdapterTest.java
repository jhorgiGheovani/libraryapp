package com.jhorgi.libraryapp.adapter.out.persistence;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.ArticleEntity;
import com.jhorgi.libraryapp.adapter.out.persistence.repository.ArticleJpaRepository;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The slice-5 branch in {@code findVisibleTo}: a role holding ARTICLE_READ_ALL
 * gets a different query, not a wider in-memory filter. The fake cannot prove
 * that the widening still happens in SQL — paging and COUNT are the tell.
 * Embedded H2 via @DataJpaTest, no Docker.
 */
@DataJpaTest
class ArticlePersistenceAdapterTest {

    private static final Long ALICE_ID = 1L;

    private static final Actor EDITOR = new Actor(9L, Role.EDITOR);
    private static final Actor CONTRIBUTOR = new Actor(9L, Role.CONTRIBUTOR);

    @Autowired
    private ArticleJpaRepository repository;

    private ArticlePersistenceAdapter adapter;

    @BeforeEach
    void seed() {
        adapter = new ArticlePersistenceAdapter(repository);
        repository.deleteAll();
        save("Alice public", Visibility.PUBLIC);
        save("Alice draft one", Visibility.PRIVATE);
        save("Alice draft two", Visibility.PRIVATE);
    }

    private void save(String title, Visibility visibility) {
        repository.save(new ArticleEntity(null, title, "Content", ALICE_ID, visibility, null, null));
    }

    @Test
    void readAllRoleSeesEveryArticleIncludingOtherPeoplesDrafts() {
        PagedResult<Article> page = adapter.findVisibleTo(EDITOR, 0, 10);

        assertThat(page.items()).extracting(Article::title)
                .containsExactlyInAnyOrder("Alice public", "Alice draft one", "Alice draft two");
        assertThat(page.totalItems()).isEqualTo(3);
    }

    @Test
    void sameCallerWithoutReadAllSeesOnlyThePublicArticle() {
        // Identical id, different role: the role is what widens the query.
        PagedResult<Article> page = adapter.findVisibleTo(CONTRIBUTOR, 0, 10);

        assertThat(page.items()).extracting(Article::title).containsExactly("Alice public");
        assertThat(page.totalItems()).isEqualTo(1);
    }

    @Test
    void theWiderListingIsStillPagedInSql() {
        // A full first page plus an accurate total is the tell that the extra
        // rows come from the query rather than from loading everything.
        PagedResult<Article> first = adapter.findVisibleTo(EDITOR, 0, 2);
        PagedResult<Article> second = adapter.findVisibleTo(EDITOR, 1, 2);

        assertThat(first.items()).hasSize(2);
        assertThat(second.items()).hasSize(1);
        assertThat(first.totalItems()).isEqualTo(3);
        assertThat(first.items()).doesNotContainAnyElementsOf(second.items());
    }

    @Test
    void deleteByAuthorIdRemovesOnlyThatAuthorsArticles() {
        // The cascade behind account deletion. Derived deletes need a live
        // transaction, which is exactly what the fake cannot prove.
        repository.save(new ArticleEntity(null, "Someone else", "Content", 42L,
                Visibility.PUBLIC, null, null));

        adapter.deleteByAuthorId(ALICE_ID);

        assertThat(repository.findAll()).extracting(ArticleEntity::getTitle)
                .containsExactly("Someone else");
    }

    @Test
    void theWiderListingIsStillNewestFirst() {
        PagedResult<Article> page = adapter.findVisibleTo(EDITOR, 0, 10);

        assertThat(page.items()).extracting(Article::title)
                .containsExactly("Alice draft two", "Alice draft one", "Alice public");
    }
}
