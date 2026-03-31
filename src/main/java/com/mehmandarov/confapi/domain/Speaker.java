package com.mehmandarov.confapi.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * A conference speaker.
 */
@Schema(description = "A conference speaker")
public class Speaker {

    @Schema(description = "Unique speaker identifier", readOnly = true)
    private String id;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    @Schema(description = "Full name", required = true, example = "Duke Java")
    private String name;

    @Size(max = 2000, message = "Bio must be at most 2000 characters")
    @Schema(description = "Speaker biography")
    private String bio;

    @Schema(description = "Company or organization", example = "OpenJDK Foundation")
    private String company;

    @Schema(description = "URL to speaker's photo")
    private String photoUrl;

    public Speaker() {
        this.id = UUID.randomUUID().toString();
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}

