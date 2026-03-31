package com.mehmandarov.confapi.gatekeepers;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Name-binding annotation — apply to resource classes or methods
 * that should have audit logging enabled.
 *
 * <pre>
 *   &#64;Audited
 *   &#64;POST
 *   public Response createSession(...) { ... }
 * </pre>
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Audited {
}

