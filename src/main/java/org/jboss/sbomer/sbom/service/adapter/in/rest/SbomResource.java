package org.jboss.sbomer.sbom.service.adapter.in.rest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.sbomer.events.common.ContextSpec;
import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.common.PublisherSpec;
import org.jboss.sbomer.events.common.Target;
import org.jboss.sbomer.events.request.RequestData;
import org.jboss.sbomer.events.request.RequestsCreated;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.GenerationRequestDTO;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.GenerationRequestsDTO;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.PublisherDTO;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.RetryResponse;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.TriggerResponse;
import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;
import org.jboss.sbomer.sbom.service.core.port.api.SbomAdministration;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationProcessor;
import org.jboss.sbomer.sbom.service.core.utility.TsidUtility;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified REST API Resource for SBOM operations.
 * Consolidates triggering, viewing, and secured admin retries.
 */
@Path("/api/v1")
@ApplicationScoped
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SbomResource {

    @Inject
    SbomAdministration sbomAdministration;

    @Inject
    GenerationProcessor generationProcessor;

    /**
     * Validates that all required dependencies are properly injected.
     * This method is called after dependency injection is complete but before the bean is put into service.
     */
    @PostConstruct
    void init() {
        Objects.requireNonNull(sbomAdministration, "SbomAdministration must be injected");
        Objects.requireNonNull(generationProcessor, "GenerationProcessor must be injected");
        log.debug("SbomResource initialized successfully with all dependencies");
    }

    @GET
    @Path("/requests")
    @Operation(summary = "List Requests", description = "Paginated list of high-level SBOM generation requests.")
    @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response fetchRequests(
            @QueryParam("pageIndex") @DefaultValue("0") @Min(value = 0, message = "Page index must be non-negative") int page,
            @QueryParam("pageSize") @DefaultValue("10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size) {
        Page<RequestRecord> result = sbomAdministration.fetchRequests(page, size);
        return Response.ok(result).build();
    }

    @GET
    @Path("/requests/{id}")
    @Operation(summary = "Get Request Details", description = "Fetch a specific SBOM generation request by ID.")
    @APIResponse(responseCode = "200", description = "Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequestRecord.class)))
    @APIResponse(responseCode = "400", description = "Invalid request ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Request not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getRequest(@PathParam("id") @NotBlank(message = "Request ID cannot be blank") String requestId) {
        RequestRecord record = sbomAdministration.getRequest(requestId);
        if (record == null) {
            throw new EntityNotFoundException("Request with ID " + requestId + " not found");
        }
        return Response.ok(record).build();
    }

    @GET
    @Path("/requests/{requestId}/generations")
    @Operation(summary = "List Generations for Request", description = "Paginated list of generations belonging to a specific request ID.")
    @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid request ID or pagination parameters", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response fetchGenerations(
            @PathParam("requestId") @NotBlank(message = "Request ID cannot be blank") String requestId,
            @QueryParam("pageIndex") @DefaultValue("0") @Min(value = 0, message = "Page index must be non-negative") int page,
            @QueryParam("pageSize") @DefaultValue("10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size) {
        Page<GenerationRecord> result = sbomAdministration.fetchGenerationsForRequest(requestId, page, size);
        return Response.ok(result).build();
    }

    @GET
    @Path("/requests/{requestId}/generations/all")
    @Operation(summary = "Fetch All Generations", description = "Get a full list of generations for a request (non-paginated).")
    @APIResponse(responseCode = "200", description = "Success - returns all generations (may be empty list)", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid request ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getAllGenerationsForRequest(
            @PathParam("requestId") @NotBlank(message = "Request ID cannot be blank") String requestId) {
        List<GenerationRecord> records = sbomAdministration.getGenerationsForRequest(requestId);
        return Response.ok(records).build();
    }

    @GET
    @Path("/generations")
    @Operation(summary = "List Generations", description = "Paginated list of generations.")
    @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response fetchGenerations(
            @QueryParam("pageIndex") @DefaultValue("0") @Min(value = 0, message = "Page index must be non-negative") int page,
            @QueryParam("pageSize") @DefaultValue("10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size) {
        Page<GenerationRecord> result = sbomAdministration.fetchGenerations(page, size);
        return Response.ok(result).build();
    }

    @GET
    @Path("/generations/{id}")
    @Operation(summary = "Get Generation Details", description = "Fetch a specific generation record by ID.")
    @APIResponse(responseCode = "200", description = "Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenerationRecord.class)))
    @APIResponse(responseCode = "400", description = "Invalid generation ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Generation not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getGeneration(
            @PathParam("id") @NotBlank(message = "Generation ID cannot be blank") String generationId) {
        GenerationRecord record = sbomAdministration.getGeneration(generationId);
        if (record == null) {
            throw new EntityNotFoundException("Generation with ID " + generationId + " not found");
        }
        return Response.ok(record).build();
    }

    // --- ACTION ENDPOINTS ---
    // todo auth
    @POST
    @Path("/generations/{id}/retry")
    @Operation(summary = "Retry Generation", description = "Resets a FAILED generation to NEW and re-schedules the event.")
    @APIResponse(responseCode = "202", description = "Retry scheduled successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RetryResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid generation ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Generation ID not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Conflict: Generation is not in FAILED state", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response retryGeneration(
            @PathParam("id") @NotBlank(message = "Generation ID cannot be blank") String generationId) {
        
        log.debug("Retry requested for generation: {}", generationId);
        
        sbomAdministration.retryGeneration(generationId);
        
        log.info("Successfully scheduled retry for generation: {}", generationId);
        
        return Response.accepted()
                .entity(new RetryResponse("Retry scheduled", generationId))
                .build();
    }

    @GET
    @Path("/enhancements/{id}")
    @Operation(summary = "Get Enhancement Details", description = "Fetch a specific enhancement record by ID.")
    @APIResponse(responseCode = "200", description = "Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EnhancementRecord.class)))
    @APIResponse(responseCode = "400", description = "Invalid enhancement ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Enhancement not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getEnhancement(
            @PathParam("id") @NotBlank(message = "Enhancement ID cannot be blank") String enhancementId) {
        EnhancementRecord record = sbomAdministration.getEnhancement(enhancementId);
        if (record == null) {
            throw new EntityNotFoundException("Enhancement with ID " + enhancementId + " not found");
        }
        return Response.ok(record).build();
    }

    @GET
    @Path("/enhancements/generation/{generationId}")
    @Operation(summary = "List Enhancements for Generation", description = "Get all enhancements for a specific generation ID.")
    @APIResponse(responseCode = "200", description = "Found", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid generation ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getEnhancementsForGeneration(
            @PathParam("generationId") @NotBlank(message = "Generation ID cannot be blank") String generationId) {
        List<EnhancementRecord> records = sbomAdministration.getEnhancementsForGeneration(generationId);
        return Response.ok(records).build();
    }

    @GET
    @Path("/enhancements")
    @Operation(summary = "List Enhancements", description = "Paginated list of enhancements.")
    @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response fetchEnhancements(
            @QueryParam("pageIndex") @DefaultValue("0") @Min(value = 0, message = "Page index must be non-negative") int page,
            @QueryParam("pageSize") @DefaultValue("10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size) {
        Page<EnhancementRecord> result = sbomAdministration.fetchEnhancements(page, size);
        return Response.ok(result).build();
    }

    // todo under auth
    @POST
    @Path("/enhancements/{id}/retry")
    @Operation(summary = "Retry Enhancement", description = "Resets a FAILED enhancement to NEW and re-schedules it using previous inputs.")
    @APIResponse(responseCode = "202", description = "Retry scheduled successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RetryResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid enhancement ID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Enhancement ID not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Conflict: Enhancement not FAILED or parent generation missing", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response retryEnhancement(
            @PathParam("id") @NotBlank(message = "Enhancement ID cannot be blank") String enhancementId) {
        
        log.debug("Retry requested for enhancement: {}", enhancementId);
        
        sbomAdministration.retryEnhancement(enhancementId);
        
        log.info("Successfully scheduled retry for enhancement: {}", enhancementId);
        
        return Response.accepted()
                .entity(new RetryResponse("Retry scheduled", enhancementId))
                .build();
    }

    @POST
    @Path("/generations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Trigger SBOM Generation", description = "Accepts a manifest of generation requests and publishers, converts them to internal events, and schedules them.")
    @APIResponse(responseCode = "202", description = "Request accepted. Returns the batch Request ID.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TriggerResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid payload or validation error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response triggerGeneration(@Valid GenerationRequestsDTO request) {

        log.info("Received REST request to trigger {} generation requests", request.generationRequests().size());

        // 1. Translate the REST DTO into the internal Avro event object
        RequestsCreated requestsCreatedEvent = toRequestsCreatedEvent(request);

        // 2. Pass the event to the core business logic (the "Port")
        generationProcessor.processGenerations(requestsCreatedEvent);

        // 3. Return a 202 Accepted response, as this is an async process.
        // We return the batch RequestId so the user can track it.
        String requestId = requestsCreatedEvent.getData().getRequestId();
        log.info("Successfully triggered generation with request ID: {}", requestId);
        
        return Response.accepted(new TriggerResponse(requestId)).build();
    }

    /**
     * Helper method to map our public DTOs to the internal Avro-generated event
     * object.
     */
    private RequestsCreated toRequestsCreatedEvent(GenerationRequestsDTO request) {
        // Validate input parameters
        if (request == null) {
            throw new ValidationException("Generation request cannot be null");
        }
        if (request.generationRequests() == null) {
            throw new ValidationException("Generation requests list cannot be null");
        }
        if (request.generationRequests().isEmpty()) {
            throw new ValidationException("At least one generation request is required");
        }

        String newRequestId = TsidUtility.createUniqueGenerationRequestId();
        // Create a new Context based on the correct Avro schema.
        ContextSpec context = ContextSpec.newBuilder()
                .setCorrelationId(newRequestId)
                .setEventId(UUID.randomUUID().toString())
                .setSource("sbomer-rest-api") // Identifies this adapter as the source
                .setEventVersion("1.0") // As per the schema default
                .setType("RequestsCreated")
                .setTimestamp(Instant.now()) // Current time in UTC millis
                .build();

        // Map Publisher DTOs to Avro PublisherSpecs
        List<PublisherSpec> publishers = Optional.ofNullable(request.publishers())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toPublisherSpec)
                .collect(Collectors.toList());

        // Map GenerationRequest DTOs to Avro GenerationRequestSpecs
        List<GenerationRequestSpec> generationRequests = request.generationRequests().stream()
                .map(this::toGenerationRequestSpec)
                .collect(Collectors.toList());

        // Create the main data spec, generating a new batch RequestId
        RequestData requestData = RequestData.newBuilder()
                .setRequestId(newRequestId)
                .setGenerationRequests(generationRequests)
                .setPublishers(publishers)
                .build();

        // Build the final event object
        return RequestsCreated.newBuilder()
                .setContext(context)
                .setData(requestData)
                .build();
    }

    private PublisherSpec toPublisherSpec(PublisherDTO dto) {
        // Validate DTO and its required fields
        if (dto == null) {
            throw new ValidationException("Publisher DTO cannot be null");
        }
        if (dto.name() == null) {
            throw new ValidationException("Publisher name cannot be null");
        }
        if (dto.version() == null) {
            throw new ValidationException("Publisher version cannot be null");
        }
        
        return PublisherSpec.newBuilder()
                .setName(dto.name())
                .setVersion(dto.version())
                .setOptions(Optional.ofNullable(dto.options()).orElse(Map.of()))
                .build();
    }

    private GenerationRequestSpec toGenerationRequestSpec(GenerationRequestDTO dto) {
        // Validate DTO and nested objects
        if (dto == null) {
            throw new ValidationException("Generation request DTO cannot be null");
        }
        if (dto.target() == null) {
            throw new ValidationException("Target cannot be null");
        }
        if (dto.target().type() == null) {
            throw new ValidationException("Target type cannot be null");
        }
        if (dto.target().identifier() == null) {
            throw new ValidationException("Target identifier cannot be null");
        }
        
        Target target = Target.newBuilder()
                .setType(dto.target().type())
                .setIdentifier(dto.target().identifier())
                .build();

        return GenerationRequestSpec.newBuilder()
                // A new, unique ID for this specific generation task
                .setGenerationId(TsidUtility.createUniqueGenerationId())
                .setTarget(target)
                .build();
    }
}
