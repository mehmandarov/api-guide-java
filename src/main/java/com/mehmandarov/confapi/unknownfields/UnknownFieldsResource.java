package com.mehmandarov.confapi.unknownfields;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo endpoint that deliberately returns MORE fields than a lean client DTO
 * (see {@link Room}) declares.
 * <p>
 * The point is to show what a JSON provider does with the extra fields when the
 * response is mapped onto a smaller record: JSON-B / Yasson ignores them by
 * default, stock Jackson throws unless told otherwise. The behaviour is
 * exercised in {@code Ch7_UnknownFieldsTest}.
 */
@Path("/unknown-fields")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Demos", description = "Standalone demos referenced from the blog")
@PermitAll
@RequestScoped
public class UnknownFieldsResource {

    @GET
    @Path("/{id}")
    @Operation(summary = "Return a room payload with extra fields beyond the lean Room DTO")
    @APIResponse(responseCode = "200", description = "An over-stuffed room payload (6 fields)")
    public Map<String, Object> getOverStuffedRoom(
            @Parameter(description = "Room ID echoed back into the payload", required = true)
            @PathParam("id") String id) {

        // A 6-field payload. A client mapping this onto the 3-field Room record
        // keeps id/name/capacity and must ignore building/floor/accessibility.
        Map<String, Object> room = new LinkedHashMap<>();
        room.put("id", id);
        room.put("name", "Hall A");
        room.put("capacity", 120);
        room.put("building", "Main");
        room.put("floor", 2);
        room.put("accessibility", Map.of("wheelchair", true));
        return room;
    }
}

