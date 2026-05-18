package org.jboss.sbomer.sbom.service.adapter.in.kafka.generation;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.request.RequestsCreated;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationProcessor;
import org.jboss.sbomer.sbom.service.core.utility.RequestValidator;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka event listener that processes generation requests.
 * Validates requests before passing to core service layer.
 */
@ApplicationScoped
@Slf4j
public class KafkaGenerationProcessor {

    private GenerationProcessor generationProcessor;
    private RequestValidator requestValidator;

    @Inject
    KafkaGenerationProcessor(
        GenerationProcessor generationProcessor,
        RequestValidator requestValidator
    ) {
        this.generationProcessor = generationProcessor;
        this.requestValidator = requestValidator;
    }

    @PostConstruct
    void init() {
        log.debug("KafkaGenerationProcessor initialized with validation");
    }

    @Incoming("requests-created")
    public void processGenerationsFromKafka(RequestsCreated requestsCreated) {
        String source = requestsCreated.getContext().getSource();
        String requestId = requestsCreated.getData().getRequestId();

        log.info("Received requests.created event from {} with requestId: {}", source, requestId);

        // Validate before processing
        List<GenerationRequestSpec> specs = requestsCreated.getData().getGenerationRequests();
        RequestValidator.ValidationResult validationResult = requestValidator.validate(specs);

        if (!validationResult.isValid()) {
            logValidationErrors(validationResult, source, requestId);
            return; // Return without processing (no entities created)
        }

        // All valid
        log.info("Validation passed. Setting up and dispatching to generators: requestId={}", requestId);
        generationProcessor.processGenerations(requestsCreated);
    }

    /**
     * Logs validation errors for debugging and monitoring.
     *
     * @param validationResult The validation result containing errors
     * @param source The event source
     * @param requestId The request ID
     */
    private void logValidationErrors(
            RequestValidator.ValidationResult validationResult,
            String source,
            String requestId) {
        log.error("Invalid requests.created event from {}: requestId={}, errors={}",
            source,
            requestId,
            validationResult.getErrors().size());

        // Log each validation error for debugging
        validationResult.getErrors().forEach(error ->
            log.error("  - Request[{}]: type={}, identifier={}, reason={}",
                error.getIndex(),
                error.getTargetType(),
                error.getTargetIdentifier(),
                error.getReason())
        );

        log.warn("Ignoring invalid requests.created event: requestId={}", requestId);
    }
}
