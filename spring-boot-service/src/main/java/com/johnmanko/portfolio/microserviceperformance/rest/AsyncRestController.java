package com.johnmanko.portfolio.microserviceperformance.rest;

import com.johnmanko.portfolio.microserviceperformance.services.MessageService;
import com.johnmanko.portfolio.microserviceperformance.util.AbstractLoggable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/async")
public class AsyncRestController extends AbstractLoggable {

    private MessageService service;

    @Autowired
    public AsyncRestController(MessageService service, ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.service = service;
    }

    @GetMapping("/cf-clean")
    public CompletableFuture<String> cfClean() {
        logThreadType();
        return CompletableFuture.completedFuture(this.service.getMessage());
    }

    @GetMapping("/cf")
    public CompletableFuture<String> cf() {
        logThreadType();
        return service.getMessageWithCompletableFutureForkJoinPool();
    }

    @GetMapping("/cf-with-async")
    public CompletableFuture<String> cfWithAsync() {
        logThreadType();
        return service.getMessageWithAsync();
    }

}
