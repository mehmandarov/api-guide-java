package com.mehmandarov.confapi.dto;

import com.mehmandarov.confapi.domain.Room;
import com.mehmandarov.confapi.domain.Session;
import com.mehmandarov.confapi.domain.SessionLevel;
import com.mehmandarov.confapi.domain.Speaker;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * V2 response DTO — enriched representation with embedded speaker &amp; room.
 * Demonstrates how a new API version can evolve the response shape
 * without breaking V1 clients.
 */
@Schema(description = "Session (V2) — enriched with embedded speaker and room objects")
public class SessionDtoV2 {

    private String id;
    private String title;
    private String sessionAbstract;
    private SessionLevel level;
    private String track;
    private LocalDateTime startTime;
    private int durationMinutes;

    @Schema(description = "Embedded speaker details")
    private EmbeddedSpeaker speaker;

    @Schema(description = "Embedded room details (null if unassigned)")
    private EmbeddedRoom room;

    public static SessionDtoV2 from(Session s, Speaker speaker, Room room) {
        SessionDtoV2 dto = new SessionDtoV2();
        dto.id = s.getId();
        dto.title = s.getTitle();
        dto.sessionAbstract = s.getSessionAbstract();
        dto.level = s.getLevel();
        dto.track = s.getTrack();
        dto.startTime = s.getStartTime();
        dto.durationMinutes = s.getDurationMinutes();

        if (speaker != null) {
            dto.speaker = new EmbeddedSpeaker(speaker.getId(), speaker.getName(), speaker.getCompany());
        }
        if (room != null) {
            dto.room = new EmbeddedRoom(room.getId(), room.getName(), room.getBuilding());
        }
        return dto;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSessionAbstract() { return sessionAbstract; }
    public SessionLevel getLevel() { return level; }
    public String getTrack() { return track; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public EmbeddedSpeaker getSpeaker() { return speaker; }
    public EmbeddedRoom getRoom() { return room; }

    // --- Nested DTOs ---

    @Schema(description = "Compact speaker info embedded in a session")
    public record EmbeddedSpeaker(String id, String name, String company) {}

    @Schema(description = "Compact room info embedded in a session")
    public record EmbeddedRoom(String id, String name, String building) {}
}

