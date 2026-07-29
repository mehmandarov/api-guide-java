package com.mehmandarov.confapi.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all mapper – turns unexpected exceptions into a 500 Problem Detail
 * <strong>without leaking implementation details</strong> to the client.
 * <p>
 * Framework {@link WebApplicationException}s (e.g. 415 Unsupported Media Type,
 * 405 Method Not Allowed) already carry a meaningful HTTP status. Those are
 * preserved and re-wrapped as a Problem Detail rather than being flattened into
 * a misleading 500 – only genuinely unexpected errors become 500.
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(CatchAllExceptionMapper.class.getName());
    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(Exception ex) {
        // A WebApplicationException carries the correct status already – honour it.
        if (ex instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            LOG.log(Level.FINE, "Mapping WebApplicationException to Problem Detail (status "
                    + status + ")", ex);

            Response.StatusType statusInfo = wae.getResponse().getStatusInfo();
            ProblemDetail problem = ProblemDetail.of(
                    status,
                    statusInfo.getReasonPhrase(),
                    ex.getMessage() != null ? ex.getMessage() : statusInfo.getReasonPhrase()
            ).withType("urn:problem-type:" + status);

            return Response.status(status)
                    .type(PROBLEM_JSON)
                    .entity(problem)
                    .build();
        }

        // Anything else is genuinely unexpected – log it and return an opaque 500.
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

