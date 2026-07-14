package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * <strong>Bonus: TUS Resumable Uploads – End-to-End</strong>
 * <p>
 * Exercises the TUS core protocol implementation over real HTTP:
 * <ul>
 *   <li>{@code POST /api/tus} – create an upload, get a {@code Location}.</li>
 *   <li>{@code PATCH /api/tus/{id}} – append chunks at the declared offset.</li>
 *   <li>{@code HEAD /api/tus/{id}} – ask where to resume after a "drop".</li>
 * </ul>
 * Companion to the blog post "Resumable file uploads with TUS and Jakarta EE".
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Bonus IT – TUS Resumable Uploads")
class Ch9_TusIT {

    private static final String TUS = "1.0.0";
    private static final String OFFSET_STREAM = "application/offset+octet-stream";

    /** Creates an upload of the given length and returns its id. */
    private String createUpload(long length) {
        String location = given()
                .header("Tus-Resumable", TUS)
                .header("Upload-Length", length)
            .when()
                .post("/api/tus")
            .then()
                .statusCode(201)
                .header("Tus-Resumable", TUS)
                .header("Location", notNullValue())
                .extract().header("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    @Nested
    @DisplayName("Happy path – upload, 'drop', resume")
    class HappyPath {

        @Test
        @DisplayName("Two PATCHes with a HEAD in between complete the upload")
        void uploadInTwoChunks() {
            byte[] chunk1 = "FIRST-HALF-BYTES-".getBytes(StandardCharsets.UTF_8);
            byte[] chunk2 = "SECOND-HALF-BYTES".getBytes(StandardCharsets.UTF_8);
            String id = createUpload(chunk1.length + chunk2.length);

            // First chunk from offset 0
            given()
                .header("Tus-Resumable", TUS)
                .header("Upload-Offset", 0)
                .contentType(OFFSET_STREAM)
                .body(chunk1)
            .when()
                .patch("/api/tus/{id}", id)
            .then()
                .statusCode(204)
                .header("Upload-Offset", equalTo(String.valueOf(chunk1.length)));

            // "Connection dropped" – ask where to resume
            long offset = Long.parseLong(given()
                    .header("Tus-Resumable", TUS)
                .when()
                    .head("/api/tus/{id}", id)
                .then()
                    .statusCode(204)
                    .header("Upload-Length", equalTo(String.valueOf(chunk1.length + chunk2.length)))
                    .extract().header("Upload-Offset"));

            // Resume from that offset
            given()
                .header("Tus-Resumable", TUS)
                .header("Upload-Offset", offset)
                .contentType(OFFSET_STREAM)
                .body(chunk2)
            .when()
                .patch("/api/tus/{id}", id)
            .then()
                .statusCode(204)
                .header("Upload-Offset", equalTo(String.valueOf(chunk1.length + chunk2.length)));
        }
    }

    @Nested
    @DisplayName("Protocol errors")
    class ProtocolErrors {

        @Test
        @DisplayName("POST without Upload-Length is rejected with 400")
        void createWithoutLength() {
            given()
                .header("Tus-Resumable", TUS)
            .when()
                .post("/api/tus")
            .then()
                .statusCode(400)
                .header("Tus-Resumable", TUS);
        }

        @Test
        @DisplayName("PATCH with a mismatching offset gets 409 plus the correct offset")
        void patchWithWrongOffset() {
            String id = createUpload(10);

            given()
                .header("Tus-Resumable", TUS)
                .header("Upload-Offset", 5) // nothing uploaded yet – real offset is 0
                .contentType(OFFSET_STREAM)
                .body("12345".getBytes(StandardCharsets.UTF_8))
            .when()
                .patch("/api/tus/{id}", id)
            .then()
                .statusCode(409)
                .header("Upload-Offset", equalTo("0"));
        }

        @Test
        @DisplayName("HEAD and PATCH on an unknown id return 404")
        void unknownUploadId() {
            given()
                .header("Tus-Resumable", TUS)
            .when()
                .head("/api/tus/{id}", "does-not-exist")
            .then()
                .statusCode(404);

            given()
                .header("Tus-Resumable", TUS)
                .header("Upload-Offset", 0)
                .contentType(OFFSET_STREAM)
                .body("x".getBytes(StandardCharsets.UTF_8))
            .when()
                .patch("/api/tus/{id}", "does-not-exist")
            .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("Capability discovery")
    class Discovery {

        @Test
        @DisplayName("OPTIONS advertises version and the creation extension")
        void optionsDiscovery() {
            given()
                .header("Tus-Resumable", TUS)
            .when()
                .options("/api/tus")
            .then()
                .statusCode(204)
                .header("Tus-Version", TUS)
                .header("Tus-Extension", "creation");
        }
    }
}
