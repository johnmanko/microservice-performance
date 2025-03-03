package com.johnmanko.portfolio.microserviceperformance.models;

public class LogEvent {

    private final String message;

    public LogEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
