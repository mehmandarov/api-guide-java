package com.mehmandarov.confapi.resource.v1;

import com.mehmandarov.confapi.domain.Room;
import com.mehmandarov.confapi.repository.RoomRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Room resource — read-only (rooms are managed by venue staff, not the API).
 */
@Path("/v1/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rooms")
@PermitAll
@RequestScoped
public class RoomResource {

    @Inject
    RoomRepository repo;

    @GET
    @Operation(summary = "List all rooms")
    @APIResponse(responseCode = "200", description = "List of rooms")
    public List<Room> listAll() {
        return repo.findAll();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get room by ID")
    @APIResponse(responseCode = "200", description = "Room found")
    @APIResponse(responseCode = "404", description = "Room not found")
    public Room getById(
            @Parameter(description = "Room ID", required = true)
            @PathParam("id") String id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found: " + id));
    }
}

