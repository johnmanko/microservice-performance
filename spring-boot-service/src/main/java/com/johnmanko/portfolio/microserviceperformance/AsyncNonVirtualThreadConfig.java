package com.johnmanko.portfolio.microserviceperformance;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 *
 * See <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html">EnableAsync</a>
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(
        value = "app.thread-executor",
        havingValue = "async"
)
public class AsyncNonVirtualThreadConfig implements AsyncConfigurer {

    @Bean
    public AsyncTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;

        /**
         * According to the @EnableAsync documentation:
         * Note: In the above example the ThreadPoolTaskExecutor is not a fully managed Spring bean. Add
         * the @Bean annotation to the getAsyncExecutor() method if you want a fully managed bean. In such
         * circumstances it is no longer necessary to manually call the executor.initialize() method as this
         * will be invoked automatically when the bean is initialized.
         */

    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                System.err.println("Exception in async method " + method.getName() + ": " + ex.getMessage());
    }
}
