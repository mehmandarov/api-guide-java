package com.mehmandarov.confapi.resource.v2;

import com.mehmandarov.confapi.domain.Room;
import com.mehmandarov.confapi.domain.Speaker;
import com.mehmandarov.confapi.dto.SessionDtoV2;
import com.mehmandarov.confapi.gatekeepers.Audited;
import com.mehmandarov.confapi.repository.RoomRepository;
import com.mehmandarov.confapi.repository.SessionRepository;
import com.mehmandarov.confapi.repository.SpeakerRepository;
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
 * V2 Session resource — enriched responses with embedded speaker &amp; room.
 * <p>
 * This is the "evolved" version: same data, richer contract.
 * V1 clients are completely unaffected.
 */
@Path("/v2/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "v2")
@Audited
@PermitAll
@RequestScoped
public class SessionResourceV2 {

    @Inject SessionRepository sessionRepo;
    @Inject SpeakerRepository speakerRepo;
    @Inject RoomRepository roomRepo;

    @GET
    @Operation(summary = "List all sessions (V2 — enriched)",
               description = "Returns sessions with embedded speaker and room objects")
    @APIResponse(responseCode = "200", description = "List of enriched sessions")
    public List<SessionDtoV2> listAll() {
        return sessionRepo.findAll().stream()
                .map(this::enrich)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get session by ID (V2 — enriched)")
    @APIResponse(responseCode = "200", description = "Enriched session found")
    @APIResponse(responseCode = "404", description = "Session not found")
    public SessionDtoV2 getById(
            @Parameter(description = "Session ID", required = true)
            @PathParam("id") String id) {
        return sessionRepo.findById(id)
                .map(this::enrich)
                .orElseThrow(() -> new NotFoundException("Session not found: " + id));
    }

    private SessionDtoV2 enrich(com.mehmandarov.confapi.domain.Session s) {
        Speaker speaker = speakerRepo.findById(s.getSpeakerId()).orElse(null);
        Room room = s.getRoomId() != null
                ? roomRepo.findById(s.getRoomId()).orElse(null)
                : null;
        return SessionDtoV2.from(s, speaker, room);
    }
}

