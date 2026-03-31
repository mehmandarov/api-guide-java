package com.mehmandarov.confapi.error;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps 403 Forbidden into an RFC 9457 Problem Detail.
 */
@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(ForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Access Denied",
                "You do not have the required role to perform this action."
        ).withType("urn:problem-type:forbidden");

        return Response.status(Response.Status.FORBIDDEN)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}

