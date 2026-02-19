package org.jboss.sbomer.test.unit.sbom.service.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.port.api.SbomAdministration;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExceptionMapperTest {

    @InjectMock
    SbomAdministration sbomAdministration;

    @Test
    void testEntityNotFoundReturnsNotFound() {
        String message = "Generation with ID gen-1 not found";
        doThrow(new EntityNotFoundException(message))
                .when(sbomAdministration).retryGeneration(anyString());
        given()
                .contentType("application/json")
                .when().post("/api/v1/generations/gen-1/retry")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body(equalTo(message));
    }

    @Test
    void testInvalidRetryStateReturnsConflict() {
        String message = "Cannot retry generation in status: GENERATING";
        doThrow(new InvalidRetryStateException(message))
                .when(sbomAdministration).retryGeneration(anyString());
        given()
                .contentType("application/json")
                .when().post("/api/v1/generations/gen-1/retry")
                .then()
                .statusCode(CONFLICT.getStatusCode())
                .body(equalTo(message));
    }

    @Test
    void testUnhandledExceptionReturnsInternalServerError() {
        String message = "Unexpected error";
        doThrow(new RuntimeException(message))
                .when(sbomAdministration).retryGeneration(anyString());
        given()
                .contentType("application/json")
                .when().post("/api/v1/generations/gen-1/retry")
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                .body(equalTo(message));
    }
}
