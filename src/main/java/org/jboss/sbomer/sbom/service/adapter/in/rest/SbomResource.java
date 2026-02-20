package org.jboss.sbomer.sbom.service.adapter.in.rest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.GenerationRequestDTO;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.GenerationRequestsDTO;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.PublisherDTO;
import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.port.api.SbomAdministration;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationProcessor;
import org.jboss.sbomer.sbom.service.core.utility.TsidUtility;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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

    @GET
    @Path("/requests")
    @Operation(summary = "List Requests", description = "Paginated list of high-level SBOM generation requests.")
    public Response fetchRequests(@QueryParam("pageIndex") @DefaultValue("0") int page,
             @QueryParam("pageSize") @DefaultValue("10") int size) {
        Page<RequestRecord> result = sbomAdministration.fetchRequests(page, size);
        return Response.ok(result).build();
    }

    @GET
    @Path("/requests/{id}")
    @Operation(summary = "Get Request Details", description = "Fetch a specific SBOM generation request by ID.")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Request not found")
    public Response getRequest(@PathParam("id") String requestId) {
        RequestRecord record = sbomAdministration.getRequest(requestId);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(record).build();
    }

    @GET
    @Path("/requests/{requestId}/generations")
    @Operation(summary = "List Generations for Request", description = "Paginated list of generations belonging to a specific request ID.")
    public Response fetchGenerations(@PathParam("requestId") String requestId,
            @QueryParam("pageIndex") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int size) {
        Page<GenerationRecord> result = sbomAdministration.fetchGenerationsForRequest(requestId, page, size);
        return Response.ok(result).build();
    }

    @GET
    @Path("/requests/{requestId}/generations/all")
    @Operation(summary = "Fetch All Generations", description = "Get a full list of generations for a request (non-paginated).")
    public Response getAllGenerationsForRequest(@PathParam("requestId") String requestId) {
        List<GenerationRecord> records = sbomAdministration.getGenerationsForRequest(requestId);

        if (records == null || records.isEmpty()) {
            // Maybe return 404 if the request ID doesn't exist,
            // just empty list 200 is okay for now
            // TODO throw an error
        }

        return Response.ok(records).build();
    }

    @GET
    @Path("/generations")
    @Operation(summary = "List Generations", description = "Paginated list of generations.")
    public Response fetchGenerations(@QueryParam("pageIndex") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int size) {
        Page<GenerationRecord> result = sbomAdministration.fetchGenerations(page, size);
        return Response.ok(result).build();
    }

    @GET
    @Path("/generations/{id}")
    @Operation(summary = "Get Generation Details", description = "Fetch a specific generation record by ID.")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Generation not found")
    public Response getGeneration(@PathParam("id") String generationId) {
        GenerationRecord record = sbomAdministration.getGeneration(generationId);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(record).build();
    }

    // --- ACTION ENDPOINTS ---
    // todo auth
    @POST
    @Path("/generations/{id}/retry")
    @Operation(summary = "Retry Generation", description = "Resets a FAILED generation to NEW and re-schedules the event.")
    @APIResponse(responseCode = "202", description = "Retry scheduled successfully")
    @APIResponse(responseCode = "404", description = "Generation ID not found")
    @APIResponse(responseCode = "409", description = "Conflict: Generation is not in FAILED state")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response retryGeneration(@PathParam("id") String generationId) {
        sbomAdministration.retryGeneration(generationId);
        return Response.accepted().entity("Retry scheduled").build();
    }

    @GET
    @Path("/enhancements/{id}")
    @Operation(summary = "Get Enhancement Details", description = "Fetch a specific enhancement record by ID.")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Enhancement not found")
    public Response getEnhancement(@PathParam("id") String enhancementId) {
        EnhancementRecord record = sbomAdministration.getEnhancement(enhancementId);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(record).build();
    }

    @GET
    @Path("/enhancements/generation/{generationId}")
    @Operation(summary = "List Enhancements for Generation", description = "Get all enhancements for a specific generation ID.")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Generation ID not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response getEnhancementsForGeneration(@PathParam("generationId") String generationId) {
        List<EnhancementRecord> records = sbomAdministration.getEnhancementsForGeneration(generationId);
        return Response.ok(records).build();
    }

    @GET
    @Path("/enhancements")
    @Operation(summary = "List Enhancements", description = "Paginated list of enhancements.")
    public Response fetchEnhancements(@QueryParam("pageIndex") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int size) {
        Page<EnhancementRecord> result = sbomAdministration.fetchEnhancements(page, size);
        return Response.ok(result).build();
    }

    // todo under auth
    @POST
    @Path("/enhancements/{id}/retry")
    @Operation(summary = "Retry Enhancement", description = "Resets a FAILED enhancement to NEW and re-schedules it using previous inputs.")
    @APIResponse(responseCode = "202", description = "Retry scheduled successfully")
    @APIResponse(responseCode = "404", description = "Enhancement ID not found")
    @APIResponse(responseCode = "409", description = "Conflict: Enhancement not FAILED or parent generation missing")
    public Response retryEnhancement(@PathParam("id") String enhancementId) {
        sbomAdministration.retryEnhancement(enhancementId);
        return Response.accepted().entity("Retry scheduled").build();
    }

    @POST
    @Path("/generations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Trigger SBOM Generation", description = "Accepts a manifest of generation requests and publishers, converts them to internal events, and schedules them.")
    @APIResponse(responseCode = "202", description = "Request accepted. Returns the batch Request ID.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(example = "{\"id\": \"req-12345\"}")))
    @APIResponse(responseCode = "400", description = "Invalid payload or validation error")
    public Response triggerGeneration(@Valid GenerationRequestsDTO request) {

        log.info("Received REST request to trigger {} generation requests", request.generationRequests().size());

        // 1. Translate the REST DTO into the internal Avro event object
        RequestsCreated requestsCreatedEvent = toRequestsCreatedEvent(request);

        // 2. Pass the event to the core business logic (the "Port")
        generationProcessor.processGenerations(requestsCreatedEvent);

        // 3. Return a 202 Accepted response, as this is an async process.
        // We return the batch RequestId so the user can track it.
        String requestId = requestsCreatedEvent.getData().getRequestId();
        return Response.accepted(Collections.singletonMap("id", requestId)).build();
    }

    /**
     * Helper method to map our public DTOs to the internal Avro-generated event
     * object.
     */
    private RequestsCreated toRequestsCreatedEvent(GenerationRequestsDTO request) {
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
        return PublisherSpec.newBuilder()
                .setName(dto.name())
                .setVersion(dto.version())
                .setOptions(Optional.ofNullable(dto.options()).orElse(Map.of()))
                .build();
    }

    private GenerationRequestSpec toGenerationRequestSpec(GenerationRequestDTO dto) {
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
