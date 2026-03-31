/**
 * <strong>Pattern 2: The Security Shield</strong>
 * <p>
 * A deep dive into JWTs, request signatures, and implementing RBAC
 * using filter patterns.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link com.mehmandarov.confapi.security.TokenClaimsFilter} — JWT claim propagation</li>
 *   <li>{@link com.mehmandarov.confapi.security.RequestSignatureFilter} — HMAC-SHA256 payload verification</li>
 *   <li>{@link com.mehmandarov.confapi.security.SignatureRequired} — opt-in binding annotation</li>
 * </ul>
 *
 * @see com.mehmandarov.confapi.ApiApplication for {@code @LoginConfig} and {@code @DeclareRoles}
 */
package com.mehmandarov.confapi.security;

