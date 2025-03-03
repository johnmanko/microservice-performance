package com.johnmanko.portfolio.microserviceperformance.services;

import com.johnmanko.portfolio.microserviceperformance.models.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LoggerService {

    @Value("${app.logging.enabled}")
    private boolean loggingEnabled;

    @Async
    @EventListener
    public void logEvent(LogEvent event) {
        if (loggingEnabled)
            System.out.println(event.getMessage());
    }

}
