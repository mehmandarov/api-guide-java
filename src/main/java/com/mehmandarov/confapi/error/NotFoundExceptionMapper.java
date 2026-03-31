package com.mehmandarov.confapi.error;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps 404 Not Found exceptions into an RFC 9457 Problem Detail.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(NotFoundException ex) {
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Resource Not Found",
                ex.getMessage() != null ? ex.getMessage() : "The requested resource does not exist."
        ).withType("urn:problem-type:not-found");

        return Response.status(Response.Status.NOT_FOUND)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}

