package com.mehmandarov.confapi.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.Map;

/**
 * Maps Bean Validation failures into a 400 Problem Detail
 * with a structured {@code violations} extension array.
 */
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(this::toMap)
                .toList();

        ProblemDetail problem = ProblemDetail.of(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Validation Failed",
                "The request body or parameters failed validation."
        ).withType("urn:problem-type:validation-error")
         .withExtension("violations", violations);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }

    private Map<String, String> toMap(ConstraintViolation<?> v) {
        String field = "";
        // Extract just the last path element as the field name
        var nodes = v.getPropertyPath().iterator();
        while (nodes.hasNext()) {
            field = nodes.next().getName();
        }
        return Map.of(
                "field", field,
                "message", v.getMessage()
        );
    }
}

