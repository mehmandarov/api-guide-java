package com.mehmandarov.confapi.error;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RFC 9457 — Problem Details for HTTP APIs.
 * <p>
 * A single, consistent error envelope used by every {@code ExceptionMapper}
 * in this application. Keeps clients happy with predictable error shapes.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a>
 */
public class ProblemDetail {

    /** A URI reference that identifies the problem type (default: "about:blank"). */
    private URI type = URI.create("about:blank");

    /** A short, human-readable summary of the problem. */
    private String title;

    /** The HTTP status code. */
    private int status;

    /** A human-readable explanation specific to this occurrence. */
    private String detail;

    /** A URI reference identifying the specific occurrence. */
    private URI instance;

    /** Extension members — arbitrary extra context (e.g. validation violations). */
    private final Map<String, Object> extensions = new LinkedHashMap<>();

    // --- Factory helpers ---

    public static ProblemDetail of(int status, String title) {
        ProblemDetail pd = new ProblemDetail();
        pd.setStatus(status);
        pd.setTitle(title);
        return pd;
    }

    public static ProblemDetail of(int status, String title, String detail) {
        ProblemDetail pd = of(status, title);
        pd.setDetail(detail);
        return pd;
    }

    public ProblemDetail withType(String typeUri) {
        this.type = URI.create(typeUri);
        return this;
    }

    public ProblemDetail withInstance(String instanceUri) {
        this.instance = URI.create(instanceUri);
        return this;
    }

    public ProblemDetail withExtension(String key, Object value) {
        this.extensions.put(key, value);
        return this;
    }

    // --- Getters & Setters ---

    public URI getType() { return type; }
    public void setType(URI type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public URI getInstance() { return instance; }
    public void setInstance(URI instance) { this.instance = instance; }

    @JsonbProperty("extensions")
    public Map<String, Object> getExtensions() { return extensions; }
}

