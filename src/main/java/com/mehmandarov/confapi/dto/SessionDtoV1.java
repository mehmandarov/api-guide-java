package com.mehmandarov.confapi.dto;

import com.mehmandarov.confapi.domain.Session;
import com.mehmandarov.confapi.domain.SessionLevel;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * V1 response DTO — flat representation with foreign-key IDs only.
 */
@Schema(description = "Session (V1) — flat representation with speaker/room IDs")
public class SessionDtoV1 {

    private String id;
    private String title;
    private String sessionAbstract;
    private SessionLevel level;
    private String track;
    private String speakerId;
    private String roomId;
    private LocalDateTime startTime;
    private int durationMinutes;

    public static SessionDtoV1 from(Session s) {
        SessionDtoV1 dto = new SessionDtoV1();
        dto.id = s.getId();
        dto.title = s.getTitle();
        dto.sessionAbstract = s.getSessionAbstract();
        dto.level = s.getLevel();
        dto.track = s.getTrack();
        dto.speakerId = s.getSpeakerId();
        dto.roomId = s.getRoomId();
        dto.startTime = s.getStartTime();
        dto.durationMinutes = s.getDurationMinutes();
        return dto;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSessionAbstract() { return sessionAbstract; }
    public SessionLevel getLevel() { return level; }
    public String getTrack() { return track; }
    public String getSpeakerId() { return speakerId; }
    public String getRoomId() { return roomId; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getDurationMinutes() { return durationMinutes; }
}

