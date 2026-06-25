# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

**sbom-service** is a core component of SBOMer NextGen, a microservice responsible for orchestrating the complete lifecycle of Software Bill of Materials (SBOM) generation and enhancement. It acts as an event-driven orchestrator that receives high-level SBOM generation requests, selects appropriate generators and enhancers based on configurable recipes, and manages the entire workflow through Kafka messaging.

### Technology Stack

- **Framework**: Quarkus 3.28.2 (Java 17)
- **Build Tool**: Maven
- **Database**: PostgreSQL with Flyway migrations
- **Messaging**: Apache Kafka with Avro schemas (Apicurio Registry)
- **Architecture Pattern**: Hexagonal Architecture (Ports and Adapters)
- **ORM**: Hibernate ORM with Panache
- **Observability**: OpenTelemetry, Micrometer, Prometheus
- **API Documentation**: OpenAPI/Swagger UI

### Key Dependencies

- `quarkus-messaging-kafka` - Kafka integration
- `quarkus-apicurio-registry-avro` - Schema registry for Avro
- `quarkus-hibernate-orm-panache` - Database access
- `quarkus-flyway` - Database migrations
- `tsid-creator` - Time-sorted unique identifiers
- `mapstruct` - DTO mapping
- `lombok` - Boilerplate reduction

## Architecture

The service follows **Hexagonal Architecture** with clear separation of concerns:

### Core Domain (`core/`)
Contains business logic for orchestrating SBOM generation and enhancement workflows. It is completely agnostic of infrastructure concerns (database, messaging, HTTP).

**Key Components:**
- **Domain Models**: DTOs and enums representing the business entities
- **Ports (Interfaces)**: Define contracts for both driving (API) and driven (SPI) adapters
- **Services**: Core business logic for workflow orchestration

### Adapters (`adapter/`)

**Primary Adapters (Driving - `adapter/in/`):**
- `rest/` - REST API endpoints for triggering generations and administration
- `kafka/` - Kafka consumers for processing status updates

**Secondary Adapters (Driven - `adapter/out/`):**
- `persistence/` - JPA repositories for database access
- `kafka/` - Kafka producers for scheduling work
- `recipe/` - Configuration-based recipe selection

### State Machine

The service manages entities through well-defined state transitions:

**Request States:**
- `PENDING` → `PROCESSING` → `COMPLETED` or `FAILED`

**Generation States:**
- `PENDING` → `GENERATING` → `COMPLETED` or `FAILED`

**Enhancement States:**
- `PENDING` → `ENHANCING` → `COMPLETED` or `FAILED`

### Event Flow

1. **Inbound**: `requests.created` - Triggers new SBOM generation workflow
2. **Outbound**: `generation.created` - Schedules generation with external generator
3. **Inbound**: `generation.update` - Receives generation status updates
4. **Outbound**: `enhancement.created` - Schedules enhancement (sequential chain)
5. **Inbound**: `enhancement.update` - Receives enhancement status updates
6. **Outbound**: `requests.finished` - Notifies completion (only on full success)
7. **Outbound**: `sbomer.errors` - Publishes error events

## Building and Running

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker/Podman (for local development)
- Minikube (for full system deployment)

### Local Development Setup

The service is designed to run within the broader SBOMer ecosystem using Helm:

```bash
# 1. Set up the local development environment (Minikube + infrastructure)
bash ./hack/setup-local-dev.sh

# 2. Build and deploy with Helm (includes this service + dependencies)
bash ./hack/run-helm-with-local-build.sh

# 3. Expose the gateway service
kubectl port-forward svc/sbomer-release-gateway 8080:8080 -n sbomer-test
```

### Build Commands

```bash
# Compile and run tests
mvn clean verify

# Build with Avro schemas (fetches sbomer-contracts)
bash ./hack/build-with-schemas.sh

# Package for deployment
mvn clean package

# Build native image (GraalVM)
mvn clean package -Pnative

# Run in dev mode (with hot reload)
mvn quarkus:dev
```

### Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run specific test class
mvn test -Dtest=SbomServiceTest
```

## Development Conventions

### Code Organization

- **Package Structure**: Follow hexagonal architecture layers strictly
  - `core/domain/` - Domain models, enums, exceptions
  - `core/port/api/` - Driving port interfaces
  - `core/port/spi/` - Driven port interfaces
  - `core/service/` - Business logic implementations
  - `adapter/in/` - Primary adapters (REST, Kafka consumers)
  - `adapter/out/` - Secondary adapters (Persistence, Kafka producers)

### Naming Conventions

- **Entities**: Suffix with `Entity` (e.g., `GenerationEntity`)
- **DTOs**: Suffix with `DTO` or `Record` (e.g., `GenerationRequestDTO`, `GenerationRecord`)
- **Repositories**: Suffix with `Repository` (e.g., `GenerationRepository`)
- **Services**: Suffix with `Service` (e.g., `SbomService`)
- **Resources**: Suffix with `Resource` (e.g., `SbomResource`)

### Database Migrations

- **Tool**: Flyway
- **Location**: `src/main/resources/db/migration/`
- **Naming**: `V<version>__<description>.sql` (e.g., `V1.0.0__Init.sql`)
- **Strategy**: Hibernate validates schema on startup; Flyway manages changes
- **Dev Data**: `import.sql` loads test data in dev mode only

### Kafka Message Handling

- **Schema Format**: Avro with Apicurio Registry
- **Consumer Groups**: Each consumer has a dedicated group ID
- **Error Handling**: 
  - **Deserialization Failures**: Configured with `failure-strategy=ignore` to prevent service crashes
  - **Dead Letter Queue**: Failed messages are sent to DLQ topics (e.g., `generation.update.dlq`)
  - **Processing Failures**: Logged and published to `sbomer.errors` topic
  - **Service Resilience**: Service continues running even when receiving invalid Avro messages
- **Idempotency**: Use TSID-based identifiers to prevent duplicate processing

### Configuration

- **Application Config**: `src/main/resources/application.properties`
- **Recipe Config**: `src/main/resources/sbomer-config.yaml`
- **Environment Variables**: 
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - Database connection
  - `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers
  - `SCHEMA_REGISTRY_URL` - Apicurio Registry URL

### Logging

- **Format**: Structured with trace context (traceId, spanId, parentId)
- **Levels**: 
  - `INFO` - Default application level
  - `DEBUG` - For `org.jboss.sbomer` package
- **MDC**: Includes OpenTelemetry trace context

### API Design

- **Base Path**: `/api/v1/`
- **Documentation**: Available at `/q/swagger-ui` (dev mode)
- **Error Responses**: Use `ErrorResponse` DTO with consistent structure
- **Pagination**: Use `Page<T>` model for list endpoints
- **Validation**: Use Hibernate Validator annotations on DTOs

### Testing Practices

- **Unit Tests**: Mock dependencies, test business logic in isolation
- **Integration Tests**: Use Testcontainers for Kafka and PostgreSQL
- **Test Database**: H2 in-memory for unit tests, PostgreSQL container for integration
- **Assertions**: Use AssertJ for fluent assertions
- **Log Capture**: Use LogCaptor for testing log output

## Key Workflows

### Triggering a Generation

```bash
# Via generic API
curl -X POST http://localhost:8080/api/v1/generations \
  -H "Content-Type: application/json" \
  -d '{
    "generationRequests": [{
      "target": {
        "type": "CONTAINER_IMAGE",
        "identifier": "quay.io/example/image:latest"
      }
    }]
  }'
```

### Monitoring Status

```bash
# List all requests
curl http://localhost:8080/api/v1/requests | jq

# View generations for a request
curl http://localhost:8080/api/v1/requests/{requestId}/generations | jq

# View enhancements for a generation
curl http://localhost:8080/api/v1/generations/{generationId}/enhancements | jq
```

### Handling Failures

The service uses a "Silent Failure" strategy - failed steps stop the chain immediately without sending final notifications. Recovery requires manual intervention via the Admin API:

```bash
# Retry a failed generation
curl -X POST http://localhost:8080/api/v1/generations/{generationId}/retry

# Retry a failed enhancement
curl -X POST http://localhost:8080/api/v1/enhancements/{enhancementId}/retry
```

## Recipe Configuration

Recipes define which generator and enhancers to use for different target types. Configuration is in `sbomer-config.yaml`:

```yaml
apiVersion: sbomer-project/v1
recipes:
  - type: CONTAINER_IMAGE
    generator:
      name: syft-generator
      version: 1.5.0
    enhancers: []
  
  - type: RPM
    generator:
      name: cyclonedx-maven-plugin
      version: 2.7.9
    enhancers:
      - name: rpm-enhancer
        version: 1.0.0
```

## Important Notes

- **TSID Usage**: All entity IDs use Time-Sorted IDs for better database performance and natural ordering
- **Transaction Boundaries**: Services are transactional; adapters handle transaction demarcation
- **Kafka Exactly-Once**: Not guaranteed; design for idempotent message processing
- **Schema Evolution**: Avro schemas must be backward compatible; coordinate with sbomer-contracts repo
- **Dev Services**: Quarkus automatically starts Kafka and PostgreSQL containers in dev mode
- **Native Builds**: Supported but require additional GraalVM configuration for reflection

## Java & Quarkus Best Practices

Follow these guidelines when writing or modifying any Java code in this repository.

### Dependency Injection

- **Prefer constructor injection** over field injection. Constructor injection makes dependencies explicit, simplifies testing, and allows fields to be `final`.
  ```java
  // ✅ Preferred
  @ApplicationScoped
  public class GenerationService {
      private final GenerationRepository repository;

      @Inject
      public GenerationService(GenerationRepository repository) {
          this.repository = repository;
      }
  }

  // ❌ Avoid
  @ApplicationScoped
  public class GenerationService {
      @Inject
      GenerationRepository repository;
  }
  ```
- Use `@ApplicationScoped` as the default CDI scope. Use `@RequestScoped` only when per-request state is genuinely needed. Avoid `@Dependent` unless the lifecycle must follow the injection point.
- Never use `new` to instantiate CDI beans — always let the container manage them.

### Immutability

- Declare fields `final` wherever possible.
- Prefer immutable value objects and records for DTOs (Java `record` types are ideal).
- Use `Collections.unmodifiableList()` / `List.copyOf()` when returning collections from beans.
- Do not expose mutable internal state through getters.

### Exception Handling

- Use **specific, meaningful exceptions** rather than catching `Exception` or `Throwable` broadly.
- Create domain-specific exception classes (e.g., `GenerationNotFoundException`) in `core/domain/exception/`.
- Never swallow exceptions silently — at minimum log them at `WARN` or `ERROR` level.
- Use Quarkus `@ServerExceptionMapper` in the REST adapter to map domain exceptions to HTTP responses consistently.
- Do not use exceptions for normal flow control.

### Lombok Usage

- Use `@Value` for immutable DTOs; use `@Data` only when mutability is genuinely required.
- Prefer `@Builder` over telescoping constructors for classes with many fields.
- Use `@Slf4j` for logger declaration — do not declare `private static final Logger` manually.
- Avoid `@SneakyThrows` — handle or declare checked exceptions explicitly.
- Do not use `@AllArgsConstructor` on CDI beans (it interferes with proxy generation); use `@RequiredArgsConstructor` with `final` fields instead.

### Code Style

- Follow standard Java naming: `camelCase` for methods/variables, `PascalCase` for types, `SCREAMING_SNAKE_CASE` for constants.
- Keep methods short and focused — a method should do one thing. If a method exceeds ~30 lines, consider extracting helper methods.
- Avoid deeply nested code (more than 2–3 levels). Use early returns (guard clauses) to reduce nesting.
- Use `Optional<T>` for return types that may be absent; never return `null` from public methods.
- Annotate overridden methods with `@Override`.
- Use `var` for local variables where the type is obvious from the right-hand side (Java 10+).

### Quarkus-Specific Idioms

- Use Panache active-record or repository pattern consistently — do not mix both within the same module.
- Annotate transactional service methods with `@Transactional`; do not annotate repository methods unless a custom transaction boundary is required.
- Use `@ConfigProperty` (or `@ConfigMapping` for groups) for injecting configuration values — never use raw `System.getProperty()`.
- Prefer `@QuarkusTest` with `@InjectMock` for unit-level tests; use `@QuarkusIntegrationTest` only for black-box integration scenarios.
- Favour reactive messaging (`@Incoming` / `@Outgoing`) over polling loops for Kafka interactions.
- Do not block the event loop in reactive code paths — offload blocking work with `@Blocking`.

### Null Safety

- Annotate parameters and return types with `@NonNull` / `@Nullable` (from Lombok or Jakarta) to document intent.
- Validate incoming public API parameters with `Objects.requireNonNull()` or Bean Validation annotations (`@NotNull`, `@NotBlank`, etc.).
- Use `Optional` instead of null-returning methods in service and domain layers.

### Logging

- Use `@Slf4j` (Lombok) — the logger field is named `log` by convention in this project.
- Log at `DEBUG` for internal state, `INFO` for significant business events, `WARN` for recoverable issues, `ERROR` for failures that require attention.
- Include structured context in log messages (entity IDs, state transitions) rather than free-form strings.
- Never log sensitive data (credentials, tokens, personal information).

### Testing

- Write tests for every non-trivial code path, including failure/edge cases.
- Use `@ExtendWith(MockitoExtension.class)` for pure unit tests that do not need CDI.
- Prefer `@InjectMocks` + `@Mock` over manual mock construction.
- Name test methods descriptively: `should<ExpectedBehaviour>_when<Condition>()`.
- Use AssertJ's `assertThat(...).isEqualTo(...)` style — avoid JUnit `assertEquals` directly.
- Do not use `Thread.sleep()` in tests — use Awaitility or reactive testing utilities.

### General Principles

- **SOLID**: Apply Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion principles.
- **DRY**: Extract duplicated logic into shared utilities or base classes, but only when the duplication is identical in intent — do not force unrelated code into a shared abstraction.
- **YAGNI**: Do not add abstractions, parameters, or generalisations "for future use". Implement only what is currently needed.
- **Fail fast**: Validate inputs at system/service boundaries as early as possible.
- **Minimal surface area**: Keep classes, methods, and fields package-private or private unless they must be public. Prefer narrow interfaces.

