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
@RequestMapping("/non-async")
public class NonAsyncRestController extends AbstractLoggable {

    private MessageService service;

    @Autowired
    public NonAsyncRestController(MessageService service, ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.service = service;
    }

    @GetMapping("/clean")
    public String clean() {
        logThreadType();
        return this.service.getMessage();
    }

    @GetMapping("/cf-get")
    public String cfGet() throws ExecutionException, InterruptedException {
        logThreadType();
        return service.getMessageWithCompletableFutureForkJoinPool().get();
    }


}
