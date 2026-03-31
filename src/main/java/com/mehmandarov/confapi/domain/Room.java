package com.mehmandarov.confapi.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * A conference room / venue.
 */
@Schema(description = "A conference room")
public class Room {

    @Schema(description = "Unique room identifier", readOnly = true)
    private String id;

    @NotBlank(message = "Room name is required")
    @Schema(description = "Room name", required = true, example = "Hall A")
    private String name;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Schema(description = "Seating capacity", example = "250")
    private int capacity;

    @Schema(description = "Building or venue name", example = "Convention Center")
    private String building;

    public Room() {
        this.id = UUID.randomUUID().toString();
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
}

