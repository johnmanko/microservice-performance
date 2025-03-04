package com.johnmanko.portfolio.microserviceperformance.services;

import com.johnmanko.portfolio.microserviceperformance.util.AbstractLoggable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class MessageService extends AbstractLoggable {

    private Executor taskExecutor;

    @Autowired
    public MessageService(Executor taskExecutor, ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.taskExecutor = taskExecutor;
    }

    public String getMessage() {
        logThreadType();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // swallow error
        }
        return "Hello, World!";
    }

    /**
     * <p>This method will manually create a CompletableFuture and return it.</p>
     * <p>It executes the task in ForkJoinPool.commonPool (JVM-managed common thread pool).</p>
     *
     * @return
     */
    public CompletableFuture<String> getMessageWithCompletableFutureForkJoinPool() {
        logThreadType();
        return CompletableFuture.supplyAsync(this::getMessage);
    }

    /**
     * <p>This method will manually create a CompletableFuture and return it.</p>
     * <p>It executes the task in Spring Boot's Executor.</p>
     * <p>By default, when you call supplyAsync() without an Executor argument, it uses ForkJoinPool.commonPool() to
     * execute the task. This behavior remains unchanged even if you annotate other methods with @Async and
     * enable @EnableAsync. The only way to change the executor for CompletableFuture.supplyAsync() is to explicitly
     * pass a custom Executor.</p>
     * <pre>
     * private final Executor taskExecutor;
     * return CompletableFuture.supplyAsync(() -> "Data fetched", taskExecutor);
     * </pre>
     * See org.springframework.scheduling.annotation.Async
     * @return
     */
    public CompletableFuture<String> getMessageWithCompletableFutureSpringExecutor() {
        logThreadType();
        return CompletableFuture.supplyAsync(this::getMessage, taskExecutor);
    }

    /**
     * <p>This method returns an already competed CompletableFuture.</p>
     * <p>When you use @EnableAsync, it enables Spring’s own task executor
     * (default: SimpleAsyncTaskExecutor or a configurable ThreadPoolTaskExecutor), but it only affects
     * methods annotated with @Async, not CompletableFuture.supplyAsync().
     * </p>
     * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html">Async</a>
     * @return
     */
    @Async
    public CompletableFuture<String> getMessageWithAsync() {
        logThreadType();
        return CompletableFuture.completedFuture(getMessage());

    }
}
