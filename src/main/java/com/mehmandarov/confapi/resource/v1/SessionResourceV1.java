package com.mehmandarov.confapi.resource.v1;

import com.mehmandarov.confapi.domain.Session;
import com.mehmandarov.confapi.dto.SessionDtoV1;
import com.mehmandarov.confapi.gatekeepers.Audited;
import com.mehmandarov.confapi.repository.SessionRepository;
import com.mehmandarov.confapi.gatekeepers.NoProfanity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

/**
 * V1 Session resource — full CRUD with flat DTOs.
 * <p>
 * Read operations are public; write operations require the ORGANIZER role.
 */
@Path("/v1/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sessions")
@Audited
@RequestScoped
public class SessionResourceV1 {

    @Inject
    SessionRepository repo;

    @GET
    @PermitAll
    @Operation(summary = "List all sessions", description = "Returns all conference sessions (V1 flat format)")
    @APIResponse(responseCode = "200", description = "List of sessions",
            content = @Content(schema = @Schema(implementation = SessionDtoV1.class)))
    public List<SessionDtoV1> listAll() {
        return repo.findAll().stream()
                .map(SessionDtoV1::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Get session by ID")
    @APIResponse(responseCode = "200", description = "Session found")
    @APIResponse(responseCode = "404", description = "Session not found")
    public SessionDtoV1 getById(
            @Parameter(description = "Session ID", required = true)
            @PathParam("id") String id) {
        return repo.findById(id)
                .map(SessionDtoV1::from)
                .orElseThrow(() -> new NotFoundException("Session not found: " + id));
    }

    @POST
    @RolesAllowed("ORGANIZER")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Create a new session")
    @APIResponse(responseCode = "201", description = "Session created")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    @APIResponse(responseCode = "403", description = "Insufficient role")
    public Response create(
            @RequestBody(description = "Session to create", required = true)
            @Valid Session session,
            @Context UriInfo uriInfo) {
        Session saved = repo.save(session);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(saved.getId()).build();
        return Response.created(location)
                .entity(SessionDtoV1.from(saved))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ORGANIZER")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Update an existing session")
    @APIResponse(responseCode = "200", description = "Session updated")
    @APIResponse(responseCode = "404", description = "Session not found")
    public SessionDtoV1 update(
            @Parameter(description = "Session ID", required = true)
            @PathParam("id") String id,
            @Valid Session session) {
        if (!repo.exists(id)) {
            throw new NotFoundException("Session not found: " + id);
        }
        session.setId(id);
        return SessionDtoV1.from(repo.save(session));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ORGANIZER")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Delete a session")
    @APIResponse(responseCode = "204", description = "Session deleted")
    @APIResponse(responseCode = "404", description = "Session not found")
    public Response delete(
            @Parameter(description = "Session ID", required = true)
            @PathParam("id") String id) {
        repo.delete(id)
                .orElseThrow(() -> new NotFoundException("Session not found: " + id));
        return Response.noContent().build();
    }
}

