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

When running locally with podman-compose, full OpenAPI documentation and Swagger UI are available to inspect DTOs and test endpoints interactively:

* **Swagger UI:** [http://localhost:8083/q/swagger-ui](http://localhost:8083/q/swagger-ui) (Port can change depending on compose mapping)
* **OpenAPI JSON:** `/q/openapi`

## Getting Started (Development)

We can run the component locally through podman-compose, which will run the component with an ephemeral Kafka and Apicurio instance:

```shell script
bash ./hack/run-compose.sh
```

Note on Persistence: Currently, the service uses an In-Memory repository. All request/generation status data will be lost if the service is restarted.

### 1. Triggering Generations
You can invoke the Errata Tool Handler's generation for an advisory, or the generic Generation API.

Errata Tool Endpoint (also deployed with podman-compose and served on localhost:8080)

```shell script
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"advisoryId": "1234"}' \
  http://localhost:8080/v1/errata-tool/generate
```

Generic Generation Endpoint: This accepts a batch of requests with specific targets (Containers, RPMs, etc.) and optional publishers.


```shell script
curl -i -X POST -H "Content-Type: application/json" \
  http://localhost:8083/api/v1/generations \
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
curl -s http://localhost:8083/api/v1/admin/requests | jq
```

View Generations for a Request:

```shell script
# Replace {requestId} with an ID from the previous command
curl -s http://localhost:8083/api/v1/admin/requests/{requestId}/generations | jq
```

### 3.Handling Failures (Retries)

The system employs a "Silent Failure" strategy. If a Generation or Enhancement fails, the chain stops immediately to prevent "Zombie" processes. No final notification is sent.

To recover, an Admin must manually retry the specific failed record.

Retry a Failed Generation: Resets status to NEW and re-schedules the generation event.

```shell script
curl -X POST http://localhost:8083/api/v1/admin/generations/{generationId}/retry
```

Retry a Failed Enhancement: Resets status to NEW, resolves the input SBOM from the previous step, and re-schedules the enhancement event.

```shell
curl -X POST http://localhost:8083/api/v1/admin/enhancements/{enhancementId}/retry
```

## Event Flow

The orchestration moves through the following states via Kafka topics:

* requests.created (Inbound Trigger)

* generation.created (Outbound -> Generator)

* generation.update (Inbound <- Generator status)

* enhancement.created (Outbound -> Enhancer [Sequential])

* enhancement.update (Inbound <- Enhancer status)

* requests.finished (Outbound -> Notification sent ONLY if all steps succeed)


## Getting Started (Local Development)

This component is designed to run alongside the wider SBOMer system using Podman Compose.

### 1. Start the Infrastructure

Run the local dev from the root of the project repository to set up the minikube environment:

```shell script
bash ./hack/setup-local-dev.sh
```

Then run the command below to install the helm chart with the component build injected into it:

```bash
bash ./hack/run-helm-with-local-build.sh
```
