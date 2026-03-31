package com.mehmandarov.confapi.error;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps authorization / authentication failures into Problem Details.
 * Covers both 401 (not authenticated) and 403 (forbidden) scenarios.
 */
@Provider
public class NotAuthorizedExceptionMapper implements ExceptionMapper<NotAuthorizedException> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(NotAuthorizedException ex) {
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.UNAUTHORIZED.getStatusCode(),
                "Authentication Required",
                "A valid JWT Bearer token is required to access this resource."
        ).withType("urn:problem-type:not-authorized");

        return Response.status(Response.Status.UNAUTHORIZED)
                .type(PROBLEM_JSON)
                .header("WWW-Authenticate", "Bearer realm=\"conference\"")
                .entity(problem)
                .build();
    }
}

