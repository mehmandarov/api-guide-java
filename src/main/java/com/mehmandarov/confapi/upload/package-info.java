/**
 * Demo for receiving binary uploads.
 * <p>
 * Companion code for the blog post "Receiving binary: REST endpoints that take
 * file uploads in Jakarta EE and Quarkus".
 * {@link com.mehmandarov.confapi.upload.UploadResource} covers the portable,
 * standard approaches ({@code EntityPart} multipart and a raw
 * {@code octet-stream} body). The Quarkus-specific {@code @RestForm} variant
 * lives as a non-compiled reference under
 * {@code snippets/QuarkusUploadResource.java}, since it only compiles on the
 * Quarkus runtime.
 */
package com.mehmandarov.confapi.upload;

