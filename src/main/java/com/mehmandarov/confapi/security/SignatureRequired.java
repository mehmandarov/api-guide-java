package com.mehmandarov.confapi.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Name-binding annotation — apply to resource methods that require
 * request payload signature verification via HMAC-SHA256.
 * <p>
 * Clients must send a {@code X-Signature} header containing the
 * HMAC-SHA256 of the request body, hex-encoded.
 *
 * @see RequestSignatureFilter
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SignatureRequired {
}

