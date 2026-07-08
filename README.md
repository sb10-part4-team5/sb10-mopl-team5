# mopl(모두의 플리)

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.5.15 |
| DB | PostgreSQL 17 + Flyway (마이그레이션) |
| 캐시/MQ | Redis 7, Kafka (Confluent, KRaft 모드) |
| 매핑 | MapStruct 1.6.3 + Lombok |
| API 문서 | springdoc-openapi (Swagger UI) |
| 빌드 | Gradle (Groovy DSL) |
| 테스트 | JUnit 5, Testcontainers, Spring Kafka Test |
| 커버리지 | Jacoco 0.8.12 |
| CI | GitHub Actions |

## 사전 요구사항

- JDK 17
- Docker Desktop 또는 Colima 등 Docker 데몬 (compose / testcontainers 모두 필요)
- IDE: IntelliJ IDEA (Lombok / MapStruct annotation processor 설정 필요)

## 실행 방법

### 1. 인프라 기동 (자동)

`build.gradle`에 `spring-boot-docker-compose`가 `developmentOnly`로 들어있어서, IDE/`bootRun` 으로 앱을 띄우면 **`docker-compose.yml`이 자동으로 실행**된다.

```bash
./gradlew bootRun
```

기동되는 컨테이너:
- `mopl-postgres` (5432)
- `mopl-redis` (6379)
- `mopl-kafka` (9092, KRaft 단일 노드)

### 2. 인프라만 따로 띄우고 싶을 때

```bash
docker compose up -d
docker compose down       # 정지
docker compose down -v    # 볼륨까지 삭제 (DB 초기화)
```

### 3. 빌드

```bash
./gradlew clean build
```

빌드 산출물: `build/libs/mopl-0.0.1-SNAPSHOT.jar`

## 프로파일 구조

| 프로파일 | 파일 위치 | 용도 |
|---|---|---|
| (공통) | `src/main/resources/application.yml` | 공통 설정, 기본 활성 프로파일 = `dev` |
| `dev` | `src/main/resources/application-dev.yml` | 로컬 개발용, 모든 인프라 `localhost` |
| `test` | `src/test/resources/application-test.yml` | 통합 테스트용, testcontainers 보조 |

## 테스트
### 커버리지 
[![codecov](https://codecov.io/gh/sb10-part4-team5/sb10-mopl-team5/branch/dev/graph/badge.svg?token=7FU4IJ3EY7)](https://codecov.io/gh/sb10-part4-team5/sb10-mopl-team5)

### 실행

```bash
./gradlew test                                # 테스트 + Jacoco 리포트 자동 생성
./gradlew test jacocoTestCoverageVerification # 커버리지 임계치 검증까지
```

리포트 경로: `build/reports/jacoco/test/html/index.html`

### 통합 테스트 작성 시 주의

1. **`@Import(TestcontainersConfiguration.class)` 필수**
   - 안 붙이면 Postgres/Redis/Kafka 컨테이너가 안 떠서 연결 실패함.

   ```java
   @SpringBootTest
   @Import(TestcontainersConfiguration.class)
   @ActiveProfiles("test")
   public abstract class IntegrationTestBase { }
   ```

2. **`application-test.yml`에 datasource/redis/kafka 연결 정보 적지 말 것**
   - `@ServiceConnection`이 testcontainers의 동적 포트/호스트를 자동 주입하는데,
     yml에 명시하면 그쪽이 우선돼서 컨테이너 연결이 깨짐.

3. **테스트에서는 docker-compose 자동 기동을 끔**
   - `application-test.yml`에 `spring.docker.compose.enabled: false` 설정돼있음.
   - 안 끄면 compose가 띄운 컨테이너와 testcontainers의 컨테이너가 포트 충돌.

4. **로컬에서 테스트 돌릴 때 Docker 데몬이 떠있어야 함**
   - IntelliJ에서 단일 테스트만 돌리는 경우에도 마찬가지.
   - 데몬 꺼져있으면 `Could not find a valid Docker environment` 에러.

## 데이터베이스 마이그레이션

- Flyway 사용. 위치: `src/main/resources/db/migration/`
- 네이밍: `V{버전}__{설명}.sql` (예: `V1__init_schema.sql`)
- `ddl-auto: validate` 모드라서 **엔티티 변경 시 마이그레이션 파일 반드시 추가**해야 부팅됨.
- 로컬에서 스키마 초기화하려면 `docker compose down -v`로 볼륨 째 삭제.

## API 문서 (Swagger)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 모니터링

Spring Actuator 활성화. 기본 노출 엔드포인트:
- `GET /actuator/health` — 헬스 체크
- `GET /actuator/info`

## CI

`.github/workflows/ci.yaml`
- 트리거: `dev`, `main` 브랜치 대상 PR
- 실행: `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification`

