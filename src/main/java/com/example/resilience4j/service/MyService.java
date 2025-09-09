package com.example.resilience4j.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

@Service
public class MyService {

    // @CircuitBreaker는 특정 메서드(callExternalService)가 예외를 발생시키거나 실패율 임계값을 초과할 때, 대신 실행할 메서드를 지정
    // 실패율이 설정값(50%)을 초과하면 서킷이 열리고, fallbackForFailure 메서드가 호출
    @CircuitBreaker(name = "myServiceCircuitBreaker", fallbackMethod = "fallbackForFailure")
    public String callExternalService() {
        // 외부 서비스 호출 시뮬레이션
        // 50% 확률로 RuntimeException을 발생시켜 실패를 유도
        if (Math.random() > 0.5) {
            throw new RuntimeException("External service failed!");
        }
        return "Success!";
    }

    private String fallbackForFailure(Throwable t) {
        return "Fallback response due to failure: " + t.getMessage();
    }

    // 메서드 실행 시간이 설정된 시간(2초)을 초과하면 타임아웃이 발생하고, fallbackForTimeout 메서드가 호출
    @TimeLimiter(name = "myServiceTimeLimiter", fallbackMethod = "fallbackForTimeout")
    public CompletableFuture<String> callSlowService() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 의도적으로 3초 지연 발생
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Slow service finished!";
        });
    }

    private CompletableFuture<String> fallbackForTimeout(Throwable t) {
        return CompletableFuture.completedFuture("Fallback response due to timeout: " + t.getMessage());
    }

    private int attemptCount = 0;

    // 재시도
    @Retry(name = "myServiceRetry", fallbackMethod = "fallbackForRetry")
    public String callWithRetry() {
        attemptCount++;
        System.out.println(LocalTime.now() + " | Attempt #" + attemptCount + ": Calling external service...");

        if (attemptCount < 3) {
            // 처음 2번은 500 에러를 반환하여 재시도를 유도합니다.
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated Server Error");
        }

        // 세 번째 시도에서 성공
        int finalAttempt = attemptCount; // 리셋하기 전에 현재 시도 횟수를 저장
        attemptCount = 0; // 다음 호출을 위해 카운트 초기화

        // 최종 시도 횟수에서 초기 호출(1회)을 제외하여 재시도 횟수를 계산
        return "Success after " + (finalAttempt - 1) + " retries!";
    }

    private String fallbackForRetry(HttpServerErrorException e) {
        System.out.println(LocalTime.now() + " | Fallback method called after all retries failed.");
        attemptCount = 0; // 재설정을 위해 카운트 초기화
        return "Fallback response: All retries failed. " + e.getMessage();
    }

    // RateLimiter를 사용하여 메서드 호출 횟수를 제한
    @RateLimiter(name = "myServiceRateLimiter", fallbackMethod = "fallbackForRateLimiter")
    public String callWithRateLimiter() {
        System.out.println(LocalTime.now() + " | A new request has been made.");
        return "Success! Rate limit is not exceeded.";
    }

    private String fallbackForRateLimiter(Exception e) {
        System.out.println(LocalTime.now() + " | Request rate limit exceeded. Falling back...");
        return "Rate limit exceeded. Please try again later.";
    }

    // 시스템의 한 부분이 실패하더라도 전체가 마비되지 않도록 자원(스레드 풀)을 격리
    @Bulkhead(name = "myServiceBulkhead", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> callWithBulkhead() {
        System.out.println(LocalTime.now() + " | A new request entered the bulkhead.");
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 이 작업은 3초 지연을 시뮬레이션
                // 이로 인해 스레드 풀이 점유되어 다른 요청이 대기하게 만듬
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(LocalTime.now() + " | Request completed.");
            return "Success! Bulkhead is working.";
        });
    }
}
