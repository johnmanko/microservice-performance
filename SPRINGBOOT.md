# Spring Boot notes.

For async operations, the Spring Boot application makes use of the `@Async` annotation, WebFlux (reactive), `CompleteableFuture`, and virtual threads.  

## Endpoints

#### `CompleteableFuture` Only

* `/async/clean` - Calls a service that returns clean `String` value, and returns that string unwrapped.
* `/non-async/cf-get` - Calls a service that returns a `CompleteableFuture`, and manually executes `get` to return the resolved value.

#### Async With `@Async` and `CompleteableFuture`

* `/async/cf-clean` - Calls a service that returns clean `String` value, and manually wraps that value in `CompletableFuture.completedFuture`.
* `/async/cf` - Calls a service that returns a `CompleteableFuture`, and returns that `Future`.
* `/async/cf-with-async` - Calls a service that's annotated with `@Async`, and returns the `CompleteableFuture` wrapper.

#### Virtual Threads

Virtual Threads are enabled via a special `@Configuration` (`AsyncVirtualThreadConfig`) , which replaces the default and Async Executor (`AsyncNonVirtualThreadConfig`)

Change configuration `app.thread-executor` to `virtual`.

#### Reactive WebFlux

* `/reactive/clean` - Calls a service that returns clean `String` value, and returns that string wrapped in a `Mono`.
* `/reactive/cf-get` - Calls a service that returns clean `CompletableFuture` value, and manually calls `get` to wrap that value in `Mono`.
* `/reactive/cf` - Calls a service that returns a `CompleteableFuture`, and manually wraps that in a `Mono.fromFuture`.
* `/reactive/cf-with-async` - Calls a service that's annotated with `@Async`, and returns the `CompleteableFuture` wrapped in `Mono.fromFuture`.


## @Async

The most important thing to remember regarding `@Async` is that it's simply and interceptor that immediately returns a `Callable` to your caller.  That's the entire magic.  I urge you to take a look at the source code for [AsyncExecutionInterceptor](https://github.com/spring-projects/spring-framework/blob/533ecf0244da4ff1dc0bdf76d51b4bae5583efb7/spring-aop/src/main/java/org/springframework/aop/interceptor/AsyncExecutionInterceptor.java#L68).

According to the [@Async JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html) (emphasis is mine):

> Annotation that marks a method as a candidate for asynchronous execution.
> 
> Can also be used at the type level, in which case all the type's methods are considered as asynchronous. Note, however, that `@Async` is not supported on methods declared within a `@Configuration` class.
> 
> In terms of target method signatures, any parameter types are supported. However, **the return type is constrained to either `void` or `java. util. concurrent.Future`**. In the latter case, you may declare the more specific `java.util.concurrent.CompletableFuture` type which allows for richer interaction with the asynchronous task and for immediate composition with further processing steps.
> 
> A `Future` handle returned from the proxy will be an actual asynchronous (`Completable`)`Future` that can be used to track the result of the asynchronous method execution. However, since the target method needs to implement the same signature, it will have to return a temporary `Future` handle that just passes a value after computation in the execution thread: typically through `java.util.concurrent.CompletableFuture.completedFuture(Object)`. The provided value will be exposed to the caller through the actual asynchronous `Future` handle at runtime.

When you annotate a method with `@Async`, Spring offloads execution to a separate thread from the configured `TaskExecutor`. Without `@Async`, the method would still execute synchronously in the calling thread when it returns a `CompletableFuture`.

So, for example, if we have a service that has an async operation, we would expect to see something like the following:

```java
@Service
public class MyService {

    /**
     * When calling this method, the Future that is returned and assigned to the caller 
     * is the one created in this method.  That means that even though the code that's running
     * inside the supplyAsync method does run asynchronously, the calling thread is blocked
     * during the entire time this method is working to build and return a Future.  In methods
     * that have a lot of setup work, this could take longer than just creating and returning
     * a CompleteableFuture.
     */
    public Future<Stirng> getSomeContent() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "Hello, World!"; 

        });
    }


    /**
     * In contrast, when you annotation a method with @Async, Spring immediately returns a Future to 
     * the caller, and runs the actual service method is a new thread.  Think of the immidatelly returned
     * Future as a wrapper future, which will then solve your returned future.
     * 
     * By default, when using CompletableFuture.supplyAsync(), it uses ForkJoinPool.commonPool, which might 
     * not be optimal for web applications. With @Async, Spring uses its configured TaskExecutor.
     */
    @Async
    public Future<Stirng> getSomeContentAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "Hello, World!"; 

        });
    }

}
```

When using raw `CompletableFuture`, exceptions must be handled manually:

```java
public CompletableFuture<String> fetchData() {
    return CompletableFuture.supplyAsync(() -> {
        throw new RuntimeException("Something went wrong");
    }).exceptionally(ex -> "Fallback data");
}
```

With `@Async`, Spring integrates exception handling via `AsyncUncaughtExceptionHandler`:

```java
@Async
public CompletableFuture<String> fetchData() {
    throw new RuntimeException("Something went wrong");
}
```

#### Async Return Types

A note about the async return type.  Spring MVC’s async processing only kicks in if the rest controller itself returns an async type it knows how to handle (such as a `CompletableFuture`, `Callable`, `DeferredResult`, or `ListenableFuture`).  So, if your REST endpoints return `Future` instead of `CompleteableFuture`, Spring will not wait for the completetion of your future.  Instead, it will pass the supplied Future object to Jackson, which will then serialize the Future object - NOT what you want!  It appears that the primary criteria is not the object type, but the endpoint's defined return type - this seems the contradict the source of [AsyncExecutionInterceptor.java#113](https://github.com/spring-projects/spring-framework/blob/533ecf0244da4ff1dc0bdf76d51b4bae5583efb7/spring-aop/src/main/java/org/springframework/aop/interceptor/AsyncExecutionInterceptor.java#L113).  I'll update this md after I investigate further.

For instance, let's say we have the following service and endpoint:

Service method:
```java
    @Async
    public Future<String> getMessageWithAsync() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // swallow error
        }
        return CompletableFuture.completedFuture("Hello, World!");

    }
```

Endpoint:
```java
    @GetMapping("/cf-with-async")
    public Future<String> cfWithAsync() {
        return service.getMessageWithAsync();
    }
```

That will yield a result of (serialization of the `Future` object):

```json
{"done":false,"cancelled":false}
```

Instead, we must specify the endpoint as returning a `CompletableFuture`:
Service method:
```java
    @Async
    public CompletableFuture<String> getMessageWithAsync() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // swallow error
        }
        return CompletableFuture.completedFuture("Hello, World!");

    }
```

Endpoint:
```java
    @GetMapping("/cf-with-async")
    public CompletableFuture<String> cfWithAsync() {
        return service.getMessageWithAsync();
    }
```

Lastly, we can annoation the rest endpoint with `@Async` in order to fork at that point, which will create a non-blocking endpoint:
```java
    @GetMapping("/cf-with-async")
    @Async
    public CompletableFuture<String> cfWithAsync() {
        return service.getMessageWithAsync();
    }
```

Beware, though.  Adding `@Async` to a controller method that returns a `CompletableFuture` typically doesn’t add any benefit.  Spring already treats asynchronous return types like `CompletableFuture` appropriately.

When a controller method returns a `CompletableFuture`, Spring recognizes it as an asynchronous type and will suspend the request processing until the future completes. This frees the servlet thread without requiring any extra annotation.

The `@Async` annotation instructs Spring to execute the method on a separate executor. While this can offload some work from the caller thread, if your method’s primary goal is to return a `CompletableFuture`, Spring's built-in support already handles the asynchronous response. In most cases, this means `@Async` is redundant on the controller method.

Let's output the thread info:

Annoted with `@Async`:

```
Thread[#133,http-nio-8080-exec-1,5,main] : org.apache.tomcat.util.threads.TaskThread
```

As you can see above, the default executor is used.

Not annotated with `@Async`:
```
Thread[#170,AsyncThread-1,5,main] : java.lang.Thread
```

Now, the above uses the async executor.

Lastly, when virtual threads is enabled in `application.properties` (`spring.thread-executor=virtual`), the async config `AsyncVirtualThreadConfig` kicks in (disabling `AsyncNonVirtualThreadConfig`).  Now, curling the endpoints yield:

Annoted with `@Async`:

```
VirtualThread[#58]/runnable@ForkJoinPool-1-worker-1 : java.lang.VirtualThread
```

Not annotated with `@Async`:
```
VirtualThread[#61]/runnable@ForkJoinPool-1-worker-2 : java.lang.VirtualThread
```

If you try to have both `AsyncVirtualThreadConfig` and `AsyncNonVirtualThreadConfig` enabled, you'll get one of the following errors:

```
java.lang.IllegalStateException: Only one AsyncConfigurer may exist
```

Note, annoationing `@Primary` on the `@Bean` method of either configurations yields the above error:
```
***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of constructor in com.johnmanko.portfolio.microserviceperformance.services.MessageService required a single bean, but 2 were found:
        - getAsyncExecutor: defined by method 'getAsyncExecutor' in class path resource [com/johnmanko/portfolio/microserviceperformance/AsyncNonVirtualThreadConfig.class]
        - applicationTaskExecutor: defined by method 'applicationTaskExecutor' in class path resource [com/johnmanko/portfolio/microserviceperformance/AsyncVirtualThreadConfig.class]

This may be due to missing parameter name information

Action:

Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, or using @Qualifier to identify the bean that should be consumed

Ensure that your compiler is configured to use the '-parameters' flag.
You may need to update both your build tool settings as well as your IDE.
(See https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-6.x#parameter-name-retention)

```

## Reactive

Another methodoloogy to handle concurrent requests is via Spring's reactive WebFlux framework.  It operative on a single thread in an event-loop manner, and can process millions of requests.  Compare that to Virtual Threads of 10,000s of requests, and Async witch is Process Thread bound.

According to ChatGPT (don't judge me! ;-):

> **WebFlux (Reactive, Non-blocking)**
> 
>   **Scalability**:
>   WebFlux is built on a non-blocking, event-loop model (using runtimes like Netty). In theory, if every component of your request processing is non-blocking, WebFlux can handle an extremely high number of concurrent requests—even reaching into the millions—because it doesn’t dedicate an OS thread per request.
> 
>   **Caveats**:
>   This level of scalability depends on keeping all I/O and processing non-blocking. If you accidentally introduce blocking calls, the benefits are lost.
> 
> --- 
>
> **Virtual Threads (Project Loom)**
> 
>   **Scalability**:
>   Virtual threads are designed to be much lighter than traditional OS threads. They allow you to write blocking code while still scaling to tens or even hundreds of thousands of concurrent tasks. The “tens of thousands” figure is a rough estimate; depending on your application’s nature, you might see even higher concurrency levels.
> 
>   **Key Point**:
>   Virtual threads bridge the gap between ease of use (blocking style) and high concurrency. However, they’re still subject to resource constraints like CPU and memory.
> 
> --- 
>
> **Traditional Async with Thread Pools**
> 
>   **Scalability**:
>   Traditional asynchronous processing (for example, using Spring’s @Async with a fixed-size ThreadPoolTaskExecutor) is indeed limited by the number of threads in the pool. Since each thread is relatively heavyweight, scaling beyond a few hundred concurrent threads is typically impractical.
> 
>   **Limitations**:
>   If your processing relies on a blocking operation, you’re effectively limited by the number of threads available. Even if you return a CompletableFuture from an @Async method, if the underlying executor has only, say, 10 or 100 threads, you’re confined to that limit.
> 
> --- 
>
> **Summary**
> 
>   **WebFlux**:
>   Optimized for high concurrency with non-blocking I/O, potentially handling millions of lightweight requests—provided every part of your stack is non-blocking.
> 
>   **Virtual Threads**:
>   Offer a balance by allowing you to write blocking code while still scaling to tens (or possibly hundreds) of thousands of concurrent tasks, thanks to their low overhead.
> 
>   **Traditional Async (@Async with Thread Pools)**:
>   Is generally limited by the size of the thread pool (i.e., the number of actual OS threads), making it less scalable compared to non-blocking approaches.

