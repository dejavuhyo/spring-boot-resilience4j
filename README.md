# Spring Boot Resilience4j

## 1. 설명
Spring Boot에서 Resilience4j를 이용하여 CircuitBreaker, TimeLimiter 및 Retry 예제이다. 포트는 8080을 사용한다.

## 2. 개발환경

* OpenJDK 21

* spring-boot 3.5.5

* spring-web 3.5.5

* lombok 1.18.38

* resilience4j-spring-boot2 2.3.0

## 3. 실행

### 1) CircuitBreaker
반복해서 호출한다. 처음에는 "Success!"와 "Fallback response..."가 번갈아 나오다가, 실패 횟수가 임계값을 넘으면 서킷이 열리면서 "Fallback response..."만 계속 반환된다.

* URL: `http://localhost:8080/call/failure`

* Method: `GET`

### 2) TimeLimiter
설정된 타임아웃(2초)을 초과하므로 즉시 "Fallback response..."가 반환된다.

* URL: `http://localhost:8080/api/call-external`

* Method: `GET`

### 3) Retry
예외를 감지하고, 설정된 maxAttempts (3회)에 따라 자동으로 재시도를 수행한다. 500 에러는 재시도를 유발하기 위한 테스트용 트리거 역할이다.

* URL: `http://localhost:8080/call/retry`

* Method: `GET`