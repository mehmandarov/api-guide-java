/**
 * Demo for resumable uploads with the TUS protocol.
 * <p>
 * Companion code for the blog post "Resumable file uploads with TUS and
 * Jakarta EE". {@link com.mehmandarov.confapi.upload.tus.TusResource}
 * implements the TUS core protocol (creation, offset lookup, append) with
 * pure {@code jakarta.ws.rs};
 * {@link com.mehmandarov.confapi.upload.tus.TusCorsFilter} adds the CORS
 * headers browser clients need;
 * {@link com.mehmandarov.confapi.upload.tus.TusDemoPageResource} serves the
 * companion HTML page at {@code /api/tus/demo} (works on every supported
 * runtime). See {@code http/tus.http} for ready-made requests and
 * {@code src/main/resources/webdemo/tus-upload-demo.html} for the browser
 * client built on tus-js-client.
 */
package com.mehmandarov.confapi.upload.tus;
