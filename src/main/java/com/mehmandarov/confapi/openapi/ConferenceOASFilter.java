package com.mehmandarov.confapi.openapi;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import java.util.List;
import java.util.Map;

/**
 * <strong>The Living Contract — OAS Filter</strong>
 * <p>
 * A MicroProfile OpenAPI filter that programmatically enhances the
 * generated OpenAPI document at build/deploy time. This is where you
 * enforce organization-wide standards without touching individual
 * resource classes:
 * <ul>
 *   <li>Adds a {@code X-Request-Id} response header to every operation</li>
 *   <li>Adds a standard {@code 500} error response to every operation</li>
 * </ul>
 * <p>
 * Registered via {@code mp.openapi.filter} in {@code microprofile-config.properties}.
 */
public class ConferenceOASFilter implements OASFilter {

    @Override
    public Operation filterOperation(Operation operation) {
        // --- Add X-Request-Id header to all success responses ---
        if (operation.getResponses() != null) {
            for (Map.Entry<String, APIResponse> entry : operation.getResponses().getAPIResponses().entrySet()) {
                APIResponse resp = entry.getValue();
                resp.addHeader("X-Request-Id",
                        org.eclipse.microprofile.openapi.OASFactory.createHeader()
                                .description("Unique request correlation ID")
                                .schema(org.eclipse.microprofile.openapi.OASFactory.createSchema()
                                        .type(List.of(Schema.SchemaType.STRING))));
            }
        }

        // --- Ensure every operation has a 500 response documented ---
        if (operation.getResponses() != null
                && !operation.getResponses().getAPIResponses().containsKey("500")) {
            operation.getResponses().addAPIResponse("500",
                    org.eclipse.microprofile.openapi.OASFactory.createAPIResponse()
                            .description("Internal Server Error — see RFC 9457 Problem Details body"));
        }

        return operation;
    }
}

