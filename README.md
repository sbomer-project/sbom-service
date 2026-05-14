# sbom-service

A core component of SBOMer NextGen responsible for orchestrating the lifecycle of SBOM generation and enhancement.

It acts as a "Hexagonal" orchestrator that:
1.  **Receives** high-level SBOM generation requests describing artifacts (Containers, GAVs, etc.).
2.  **Selects** the appropriate Generator and Enhancers based on internal recipes.
3.  **Orchestrates** the workflow via an event-driven architecture (Kafka).
4.  **Manages State** (New, Generating, Enhancing, Finished, Failed).
5.  **Recovers** from failures via surgical Admin API retries.

## Architecture

The service follows **Hexagonal Architecture (Ports and Adapters)**:
* **Core Domain:** Contains business logic for sequencing generations and enhancements. It is agnostic of the database or message broker.
* **Primary Ports (Driving):** REST APIs for triggering generations (`GenerationResource`) and Administration (`SbomAdminResource`).
* **Secondary Ports (Driven):** Adapters for Persistence (`StatusRepository`) and Messaging (`GenerationScheduler`, `EnhancementScheduler`).

## API Documentation

When running locally with helm, full OpenAPI documentation and Swagger UI are available to inspect DTOs and test endpoints interactively:

* **Swagger UI:** [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui) (Port can change depending on helm mapping)
* **OpenAPI JSON:** `/q/openapi`

## Getting Started (Local Development)

This component is designed to run alongside the wider SBOMer system using Helm.

### 0. Start the Infrastructure

Run the local dev from the root of the project repository to set up the minikube environment:

```shell script
bash ./hack/setup-local-dev.sh
```

Then run the command below to install the helm chart with other system components and with the component build injected into it:

```bash
bash ./hack/run-helm-with-local-build.sh
```
and expose the gateway service to access the APIs:

```
kubectl port-forward svc/sbomer-release-gateway 8080:8080 -n sbomer-test
```



### 1. Triggering Generations
You can invoke the Errata Tool Handler's generation for an advisory, or the generic Generation API.

Errata Tool Endpoint (also deployed with helm and served on localhost:8080)

```shell script
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"advisoryId": "1234"}' \
  http://localhost:8080/v1/errata-tool/generate
```

Generic Generation Endpoint: This accepts a batch of requests with specific targets (Containers, RPMs, etc.) and optional publishers.


```shell script
curl -i -X POST -H "Content-Type: application/json" \
  http://localhost:8080/api/v1/generations \
  -d '{
    "generationRequests": [
      {
        "target": {
          "type": "CONTAINER_IMAGE",
          "identifier": "quay.io/pct-security/mequal:latest"
        }
      }
    ],
    "publishers": []
  }'
```

### 2. Administration & Status

The service provides a dedicated Admin API to view the progress of the generation and enhancements, and manually retry failed steps.

List all Requests:

```shell script
curl -s http://localhost:8080/api/v1/requests | jq
```

View Generations for a Request:

```shell script
# Replace {requestId} with an ID from the previous command
curl -s http://localhost:8080/api/v1/requests/{requestId}/generations | jq
```

### 3. Handling Failures (Retries)

The system employs a "Silent Failure" strategy. If a Generation or Enhancement fails, the chain stops immediately to prevent "Zombie" processes. No final notification is sent.

#### Automatic Retries

The service supports **automatic immediate retry** for transient failures. When enabled, failed generations and enhancements are automatically retried based on canonical error type configuration.

**Canonical Error System:**

The service uses a **canonical error classification system** (`ErrorResult` enum) that provides stable, service-owned error codes independent of external tool implementations. This ensures consistent retry behavior regardless of which generator or enhancer is used.

**Error Categories:**
- **VALIDATION** - Client input errors (e.g., `INVALID_TARGET`, `INVALID_REQUEST`) - Not retryable
- **CONFIGURATION** - Setup/config issues (e.g., `CONFIG_MISSING`, `SCHEMA_REGISTRY_ERROR`) - Mixed retryability
- **ORCHESTRATION** - Service workflow errors (e.g., `GENERATION_SCHEDULING_ERROR`) - Usually retryable
- **EXTERNAL_EXECUTION** - Generator/enhancer failures (e.g., `GENERATOR_EXECUTION_FAILED`, `EXTERNAL_RESOURCE_EXHAUSTED`) - Usually retryable
- **INTERNAL** - Service bugs or infrastructure (e.g., `DATABASE_ERROR`, `UNEXPECTED_ERROR`) - Mixed retryability

**Configuration** (`application.properties`):

```properties
# Enable/disable automatic retry globally
sbomer.retry.enabled=true

# Retryable errors (transient failures - infrastructure/platform issues)
sbomer.retry.generation.EXTERNAL_RESOURCE_EXHAUSTED.max-attempts=3
sbomer.retry.generation.EXTERNAL_SYSTEM_ERROR.max-attempts=3
sbomer.retry.generation.GENERATOR_EXECUTION_FAILED.max-attempts=2
sbomer.retry.generation.EXTERNAL_TIMEOUT.max-attempts=3

# Non-retryable errors (permanent failures - validation/configuration issues)
sbomer.retry.generation.EXTERNAL_BAD_CONFIGURATION.max-attempts=0
sbomer.retry.generation.INVALID_TARGET.max-attempts=0
sbomer.retry.generation.INVALID_REQUEST.max-attempts=0
```

**Legacy Error Mapping:**

External workers may still report legacy error codes (e.g., `ERR_OOM`, `ERR_SYSTEM`). The service automatically maps these to canonical codes:
- `ERR_OOM` → `EXTERNAL_RESOURCE_EXHAUSTED`
- `ERR_SYSTEM` → `EXTERNAL_SYSTEM_ERROR`
- `ERR_INDEX_INVALID` → `INVALID_TARGET`
- `ERR_GENERATION` → `GENERATOR_EXECUTION_FAILED`
- `ERR_ENHANCEMENT` → `ENHANCER_EXECUTION_FAILED`
- `ERR_CONFIG_INVALID` → `EXTERNAL_BAD_CONFIGURATION`

**How It Works:**
- When a generation/enhancement fails, the service maps the error to a canonical code
- The canonical code determines if the error is retryable based on configuration
- If retry is allowed and max attempts not reached, a new run is created immediately
- Retries are transparent to generators/enhancers (they see normal requests)
- Each retry increments the attempt number tracked in the Run entity
- Original upstream error reasons are preserved in `upstream_reason` field for diagnostics

**Monitoring Retries:**

View all runs for a generation (including retries):
```shell script
curl -s http://localhost:8080/api/v1/generations/{generationId}/runs | jq
```

Query retry statistics:
```sql
-- Find generations with multiple attempts
SELECT generation_id, COUNT(*) as attempts
FROM generation_run
GROUP BY generation_id
HAVING COUNT(*) > 1;
```

#### Manual Retries

For cases where automatic retry is disabled or exhausted, admins can manually retry failed records.

Retry a Failed Generation: Resets status to NEW and re-schedules the generation event.

```shell script
curl -X POST http://localhost:8080/api/v1/generations/{generationId}/retry
```

Retry a Failed Enhancement: Resets status to NEW, resolves the input SBOM from the previous step, and re-schedules the enhancement event.

```shell
curl -X POST http://localhost:8080/api/v1/enhancements/{enhancementId}/retry
```

## Event Flow

The orchestration moves through the following states via Kafka topics:

* requests.created (Inbound Trigger)

* generation.created (Outbound -> Generator)

* generation.update (Inbound <- Generator status)

* enhancement.created (Outbound -> Enhancer [Sequential])

* enhancement.update (Inbound <- Enhancer status)

* requests.finished (Outbound -> Notification sent ONLY if all steps succeed)


