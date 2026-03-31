package com.mehmandarov.confapi.resource.v1;

import com.mehmandarov.confapi.domain.Speaker;
import com.mehmandarov.confapi.gatekeepers.Audited;
import com.mehmandarov.confapi.repository.SpeakerRepository;
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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

/**
 * Speaker resource — CRUD for conference speakers.
 */
@Path("/v1/speakers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Speakers")
@Audited
@RequestScoped
public class SpeakerResource {

    @Inject
    SpeakerRepository repo;

    @GET
    @PermitAll
    @Operation(summary = "List all speakers")
    @APIResponse(responseCode = "200", description = "List of speakers")
    public List<Speaker> listAll() {
        return repo.findAll();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Get speaker by ID")
    @APIResponse(responseCode = "200", description = "Speaker found")
    @APIResponse(responseCode = "404", description = "Speaker not found")
    public Speaker getById(
            @Parameter(description = "Speaker ID", required = true)
            @PathParam("id") String id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Speaker not found: " + id));
    }

    @POST
    @RolesAllowed("ORGANIZER")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Create a new speaker")
    @APIResponse(responseCode = "201", description = "Speaker created")
    @APIResponse(responseCode = "400", description = "Validation error")
    public Response create(@Valid Speaker speaker, @Context UriInfo uriInfo) {
        Speaker saved = repo.save(speaker);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(saved.getId()).build();
        return Response.created(location).entity(saved).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ORGANIZER")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Update a speaker")
    @APIResponse(responseCode = "200", description = "Speaker updated")
    @APIResponse(responseCode = "404", description = "Speaker not found")
    public Speaker update(
            @PathParam("id") String id,
            @Valid Speaker speaker) {
        repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Speaker not found: " + id));
        speaker.setId(id);
        return repo.save(speaker);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ORGANIZER")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Delete a speaker")
    @APIResponse(responseCode = "204", description = "Speaker deleted")
    @APIResponse(responseCode = "404", description = "Speaker not found")
    public Response delete(@PathParam("id") String id) {
        repo.delete(id)
                .orElseThrow(() -> new NotFoundException("Speaker not found: " + id));
        return Response.noContent().build();
    }
}

