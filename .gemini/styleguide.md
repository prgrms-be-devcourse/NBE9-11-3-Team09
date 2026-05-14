# ParkEasy Code Style Guide

## 기술 스택
- Spring Boot 4.x
- Kotlin (Java에서 마이그레이션 진행 중)
- JPA / Hibernate
- QueryDSL (KSP 기반)
- Spring Security / JWT

## 코드 컨벤션
- Kotlin 전환 중이므로 Java/Kotlin 혼용 코드 리뷰 시 참고
- Kotlin 파일은 data class, val 우선 사용
- @Transactional(readOnly = true) 기본 적용
- 예외는 IllegalArgumentException, IllegalStateException 사용
- 한국어 주석 허용

## 리뷰 집중 영역
- 동시성 제어 (CAS, 비관적 락)
- N+1 문제
- 트랜잭션 범위
- Null Safety