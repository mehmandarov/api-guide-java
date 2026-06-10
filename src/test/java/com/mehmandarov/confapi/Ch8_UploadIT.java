package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Bonus: Binary Uploads — End-to-End</strong>
 * <p>
 * Exercises the portable {@code jakarta.ws.rs} upload endpoints over real HTTP:
 * <ul>
 *   <li>{@code POST /api/uploads/multipart} — a {@code multipart/form-data}
 *       request with a text part and a binary part, read via {@code EntityPart}.</li>
 *   <li>{@code POST /api/uploads/raw} — a single binary body
 *       ({@code application/octet-stream}); the body <em>is</em> the file.</li>
 * </ul>
 * Companion to the blog post "Receiving binary: REST endpoints that take file
 * uploads in Jakarta EE and Quarkus". The Quarkus {@code @RestForm} variant
 * ({@code snippets/QuarkusUploadResource.java}) is intentionally not covered
 * here — it is a reference snippet that is not part of the default build.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Bonus IT — Binary Uploads")
class Ch8_UploadIT {

    @Nested
    @DisplayName("Multipart — standard EntityPart")
    class Multipart {

        @Test
        @DisplayName("POST /api/uploads/multipart echoes file name, byte count, and description")
        void multipartUpload() {
            byte[] fileBytes = "PDF-BYTES-PRETEND".getBytes(StandardCharsets.UTF_8);

            given()
                .multiPart("description", "Conference floor plan")
                .multiPart("file", "floorplan.pdf", fileBytes, "application/pdf")
            .when()
                .post("/api/uploads/multipart")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("fileName", is("floorplan.pdf"))
                .body("bytes", is(fileBytes.length))
                .body("description", is("Conference floor plan"));
        }

        @Test
        @DisplayName("Missing description part defaults to empty string, file still read")
        void multipartWithoutDescription() {
            byte[] fileBytes = "abc".getBytes(StandardCharsets.UTF_8);

            given()
                .multiPart("file", "notes.bin", fileBytes, "application/octet-stream")
            .when()
                .post("/api/uploads/multipart")
            .then()
                .statusCode(200)
                .body("fileName", is("notes.bin"))
                .body("bytes", is(fileBytes.length))
                .body("description", is(""));
        }
    }

    @Nested
    @DisplayName("Raw — application/octet-stream body")
    class Raw {

        @Test
        @DisplayName("POST /api/uploads/raw echoes header metadata and byte count")
        void rawUpload() {
            byte[] body = "PNG-BYTES-PRETEND".getBytes(StandardCharsets.UTF_8);

            given()
                .contentType("image/png")
                .header("X-File-Name", "headshot.png")
                .body(body)
            .when()
                .post("/api/uploads/raw")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("fileName", is("headshot.png"))
                .body("contentType", is("image/png"))
                .body("bytes", is(body.length));
        }

        @Test
        @DisplayName("Missing X-File-Name header falls back to 'unnamed'")
        void rawUploadWithoutFileName() {
            byte[] body = "data".getBytes(StandardCharsets.UTF_8);

            given()
                .contentType(ContentType.BINARY)
                .body(body)
            .when()
                .post("/api/uploads/raw")
            .then()
                .statusCode(200)
                .body("fileName", is("unnamed"))
                .body("bytes", is(body.length));
        }
    }
}

