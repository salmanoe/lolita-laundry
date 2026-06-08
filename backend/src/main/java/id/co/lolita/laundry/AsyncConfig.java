package id.co.lolita.laundry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Backs every {@code @Async} method — in this app that is exclusively the billing module's
 * {@code @ApplicationModuleListener}s ({@code OrderBillingSyncEvent} → monthly-billing upsert,
 * {@code OrderDeliveredEvent} → order invoice) — with a <strong>single-threaded</strong>
 * executor, so events are processed strictly one at a time.
 *
 * <p><strong>Why single-threaded:</strong> the billing sync re-reads the order and read-modify-
 * writes the period's DRAFT billing aggregate in its own transaction. Two events for the
 * <em>same order</em> processed concurrently (e.g. an order created and immediately edited) each
 * read independently and can lose an update — the later commit overwriting the other with a
 * stale subtotal. Serializing all billing event handling removes that race without an
 * optimistic-lock column. At this scale (one instance, a handful of users) the throughput cost
 * is irrelevant; correctness is what matters. The queue is unbounded so nothing is dropped, and
 * Modulith's JPA event registry still persists + retries each publication across a restart.
 *
 * <p>Providing {@link AsyncConfigurer#getAsyncExecutor()} sets this as the default executor
 * resolved by {@code @Async}, so the {@code @ApplicationModuleListener}s pick it up without any
 * per-listener qualifier.
 */
@Configuration
@EnableAsync
class AsyncConfig implements AsyncConfigurer {

    @Bean
    ThreadPoolTaskExecutor billingEventExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(Integer.MAX_VALUE);   // unbounded — never reject a billing event
        executor.setThreadNamePrefix("billing-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return billingEventExecutor();
    }
}