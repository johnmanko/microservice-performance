package com.johnmanko.portfolio.microserviceperformance.rest;

import com.johnmanko.portfolio.microserviceperformance.services.MessageService;
import com.johnmanko.portfolio.microserviceperformance.util.AbstractLoggable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/reactive")
public class ReactiveRestController extends AbstractLoggable {

    private MessageService service;

    @Autowired
    public ReactiveRestController(MessageService service, ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.service = service;
    }

    @GetMapping("/clean")
    public Mono<String> clean() {
        logThreadType();
        return Mono.just(service.getMessage());
    }

    @GetMapping("/cf-get")
    public Mono<String> cfGet() throws ExecutionException, InterruptedException {
        logThreadType();
        return Mono.just(service.getMessageWithCompletableFutureForkJoinPool().get());
    }

    @GetMapping("/cf")
    public Mono<String> cf() {
        logThreadType();
        return Mono.fromFuture(service.getMessageWithCompletableFutureForkJoinPool());
    }

    @GetMapping("/cf-with-async")
    public Mono<String> cfWithAsync() {
        logThreadType();
        return Mono.fromFuture(service.getMessageWithAsync());
    }

}
