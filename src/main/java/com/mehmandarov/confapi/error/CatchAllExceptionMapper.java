package com.mehmandarov.confapi.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all mapper — turns unexpected exceptions into a 500 Problem Detail
 * <strong>without leaking implementation details</strong> to the client.
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(CatchAllExceptionMapper.class.getName());
    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(Exception ex) {
        // Always log the real error server-side
        LOG.log(Level.SEVERE, "Unhandled exception", ex);

        ProblemDetail problem = ProblemDetail.of(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."
        ).withType("urn:problem-type:internal-error");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}

