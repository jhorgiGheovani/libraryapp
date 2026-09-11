package com.jhorgi.libraryapp.application.policy;

import com.jhorgi.libraryapp.domain.exception.ArticleAccessDeniedException;
import com.jhorgi.libraryapp.domain.exception.ArticleNotFoundException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.Permission;

import java.util.Optional;

/**
 * The single decision point C1 asked for, now role-aware. Every read and every
 * write goes through here, so no endpoint can skip the visibility rule or the
 * role matrix.
 *
 * <p>Order matters: visibility is checked before the role gate, so a caller who
 * cannot see an article gets 404 rather than a 403 that would confirm the id
 * exists. Same anti-oracle stance as the 423 on login and the flat OTP failure.
 */
public final class ArticlePolicy {

    private ArticlePolicy() {
    }

    /** 404 when the article does not exist <em>or</em> is not visible to the caller. */
    public static Article readable(Optional<Article> found, Actor actor) {
        Article article = found.orElseThrow(ArticleNotFoundException::new);
        if (!isVisibleTo(article, actor)) {
            throw new ArticleNotFoundException();
        }
        return article;
    }

    public static boolean isVisibleTo(Article article, Actor actor) {
        return article.isPublic()
                || actor.owns(article)
                || actor.can(Permission.ARTICLE_READ_ALL);
    }

    public static void requireCanCreate(Actor actor) {
        if (!actor.can(Permission.ARTICLE_CREATE)) {
            throw new ArticleAccessDeniedException();
        }
    }

    public static void requireCanUpdate(Article article, Actor actor) {
        requireWrite(article, actor, Permission.ARTICLE_UPDATE_OWN);
    }

    public static void requireCanDelete(Article article, Actor actor) {
        requireWrite(article, actor, Permission.ARTICLE_DELETE_OWN);
    }

    /**
     * ARTICLE_WRITE_ANY short-circuits ownership entirely (Super Admin). Everyone
     * else needs both the own-article permission and actual ownership — which is
     * how a Contributor is refused a delete on their own post.
     */
    private static void requireWrite(Article article, Actor actor, Permission onOwn) {
        if (actor.can(Permission.ARTICLE_WRITE_ANY)) {
            return;
        }
        if (!actor.can(onOwn) || !actor.owns(article)) {
            throw new ArticleAccessDeniedException();
        }
    }
}
