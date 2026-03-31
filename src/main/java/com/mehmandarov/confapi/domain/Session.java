package com.mehmandarov.confapi.domain;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.mehmandarov.confapi.gatekeepers.NoProfanity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A conference session (talk, workshop, keynote).
 */
@Schema(description = "A conference session")
public class Session {

    @Schema(description = "Unique session identifier", readOnly = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    @NoProfanity
    @Schema(description = "Session title", required = true, example = "An Opinionated Guide to Bulletproof APIs with Java")
    private String title;

    @NotBlank(message = "Abstract is required")
    @Size(max = 2000, message = "Abstract must be at most 2000 characters")
    @Schema(description = "Session abstract / description", required = true)
    @JsonbProperty("abstract")
    private String sessionAbstract;

    @NotNull(message = "Level is required")
    @Schema(description = "Difficulty level", required = true, example = "INTERMEDIATE")
    private SessionLevel level;

    @Size(max = 100, message = "Track name must be at most 100 characters")
    @Schema(description = "Conference track", example = "Backend & APIs")
    private String track;

    @NotBlank(message = "Speaker ID is required")
    @Schema(description = "ID of the speaker delivering this session", required = true)
    private String speakerId;

    @Schema(description = "ID of the assigned room")
    private String roomId;

    @FutureOrPresent(message = "Start time must be in the future")
    @Schema(description = "Session start time", example = "2026-10-15T09:00:00")
    private LocalDateTime startTime;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Schema(description = "Duration in minutes", example = "50")
    private int durationMinutes;

    public Session() {
        this.id = UUID.randomUUID().toString();
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSessionAbstract() { return sessionAbstract; }
    public void setSessionAbstract(String sessionAbstract) { this.sessionAbstract = sessionAbstract; }

    public SessionLevel getLevel() { return level; }
    public void setLevel(SessionLevel level) { this.level = level; }

    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }

    public String getSpeakerId() { return speakerId; }
    public void setSpeakerId(String speakerId) { this.speakerId = speakerId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}
