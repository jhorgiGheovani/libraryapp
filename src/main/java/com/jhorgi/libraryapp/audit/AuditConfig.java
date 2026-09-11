package com.jhorgi.libraryapp.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AuditConfig {

    public static final String AUDIT_EXECUTOR = "auditExecutor";

    /**
     * Registered explicitly rather than as a {@code @Component}, for two reasons:
     * the order below is not the default one, and a {@code OncePerRequestFilter}
     * that is also a bean gets picked up by Boot's auto-registration as well,
     * which would install it twice.
     */
    @Bean
    public FilterRegistrationBean<AuditContextFilter> auditContextFilter(
            @Value("${audit.trust-forwarded-for:false}") boolean trustForwardedFor) {

        FilterRegistrationBean<AuditContextFilter> registration =
                new FilterRegistrationBean<>(new AuditContextFilter(trustForwardedFor));
        // First in the chain: anything that throws later — a rejected token, a 403
        // from method security — still needs the address and user-agent available.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * A pool of its own, not the common one. Audit writes must never queue behind
     * unrelated {@code @Async} work, and — more importantly — must never be able
     * to starve it either.
     *
     * <p>{@code CallerRunsPolicy} on saturation: under a burst the entry is
     * written on the calling thread instead. That trades a slower request for a
     * complete trail, which is the right way round for a security log. Silently
     * dropping the row is the one outcome that must not happen.
     */
    @Bean(name = AUDIT_EXECUTOR)
    public TaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Let a shutdown finish flushing the queue rather than losing what is in it.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
