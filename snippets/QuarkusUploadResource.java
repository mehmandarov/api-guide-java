// ============================================================================
// REFERENCE SNIPPET - NOT COMPILED BY THE DEFAULT BUILD.
//
// This is the Quarkus-specific variant of the binary-upload endpoint, kept out
// of src/main/java on purpose: it depends on RESTEasy Reactive types
// (org.jboss.resteasy.reactive.RestForm and
// org.jboss.resteasy.reactive.multipart.FileUpload) that are only on the
// classpath under the -Pquarkus profile. The portable, always-compiled version
// lives in src/main/java/com/mehmandarov/confapi/upload/UploadResource.java and
// uses the standard Jakarta REST EntityPart API instead.
//
// To run it on Quarkus: move this file into
//   src/main/java/com/mehmandarov/confapi/upload/
// build with `-Pquarkus`, and make sure the multipart support is present
// (quarkus-rest already provides it). See https://quarkus.io/guides/rest
// ============================================================================

package com.mehmandarov.confapi.upload;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quarkus / RESTEasy Reactive flavour of the multipart upload endpoint.
 * <p>
 * Each part binds directly to a parameter with {@code @RestForm}; a binary part
 * maps to a {@link FileUpload} (Quarkus has already streamed it to a temp file
 * by the time the method runs).
 */
@Path("/uploads")
@PermitAll
@RequestScoped
public class QuarkusUploadResource {

    @POST
    @Path("/quarkus")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> upload(
            @RestForm("description") String description,
            @RestForm("file") FileUpload file) {

        // file.uploadedFile() is a Path to a temp file Quarkus already wrote.
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fileName", file.fileName());
        summary.put("size", file.size());
        summary.put("contentType", file.contentType());
        summary.put("description", description == null ? "" : description);
        return summary;
    }
}

