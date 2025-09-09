package com.example.resilience4j.controller;

import com.example.resilience4j.service.MyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
public class MyController {

    @Autowired
    private MyService myService;

    @GetMapping("/call/failure")
    public String handleFailure() {
        return myService.callExternalService();
    }

    @GetMapping("/call/timeout")
    public String handleTimeout() throws ExecutionException, InterruptedException {
        return myService.callSlowService().get();
    }

    @GetMapping("/call/retry")
    public String handleRetry() {
        return myService.callWithRetry();
    }

    @GetMapping("/call/ratelimiter")
    public String handleRateLimiter() {
        return myService.callWithRateLimiter();
    }

    @GetMapping("/call/bulkhead")
    public String handleBulkhead() {
        try {
            return myService.callWithBulkhead().get();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
