package com.example.resilience4j.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

@Service
public class MyService {

    private int attemptCount = 0;

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

    // 실패 시 대체 로직
    private String fallbackForFailure(Throwable t) {
        return "Fallback response due to failure: " + t.getMessage();
    }

    // 메서드에 타임 리미터 적용
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

    // 실패 시 대체 로직
    private CompletableFuture<String> fallbackForTimeout(Throwable t) {
        return CompletableFuture.completedFuture("Fallback response due to timeout: " + t.getMessage());
    }

    //  재시도 패턴
    @Retry(name = "myServiceRetry", fallbackMethod = "fallbackForRetry")
    public String callWithRetry() {
        attemptCount++;
        System.out.println(LocalTime.now() + " | Attempt #" + attemptCount + ": Calling external service...");

        if (attemptCount < 3) {
            // 처음 2번은 500 에러를 반환하여 재시도를 유도합니다.
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated Server Error");
        }

        // 세 번째 시도에서는 성공
        attemptCount = 0; // 재설정을 위해 카운트 초기화
        return "Success after " + attemptCount + " retries!";
    }

    // 실패 시 대체 로직
    private String fallbackForRetry(HttpServerErrorException e) {
        System.out.println(LocalTime.now() + " | Fallback method called after all retries failed.");
        attemptCount = 0; // 재설정을 위해 카운트 초기화
        return "Fallback response: All retries failed. " + e.getMessage();
    }
}
