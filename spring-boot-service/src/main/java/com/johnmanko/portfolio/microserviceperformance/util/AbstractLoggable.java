package com.johnmanko.portfolio.microserviceperformance.util;

import com.johnmanko.portfolio.microserviceperformance.models.LogEvent;
import org.springframework.context.ApplicationEventPublisher;

public abstract class AbstractLoggable {

    private final ApplicationEventPublisher eventPublisher;

    public AbstractLoggable(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void logMessage(String message) {
        this.eventPublisher.publishEvent(new LogEvent(message));
    }

    protected void logThreadType() {
        this.logMessage(String.format("%s : %s", Thread.currentThread().toString(), Thread.currentThread().getClass().getName()));
    }

}
