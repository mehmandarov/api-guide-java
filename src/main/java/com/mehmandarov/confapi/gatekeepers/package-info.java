/**
 * <strong>Pattern 1: The Gatekeepers</strong>
 * <p>
 * Request filters for transparent input sanitization, validation, and auditing
 * — so your business logic stays clean.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link com.mehmandarov.confapi.gatekeepers.InputSanitizationFilter} — {@code @PreMatching} query-param sanitizer</li>
 *   <li>{@link com.mehmandarov.confapi.gatekeepers.InputSanitizationInterceptor} — {@code ReaderInterceptor} for body sanitization</li>
 *   <li>{@link com.mehmandarov.confapi.gatekeepers.AuditFilter} — name-bound audit logging</li>
 *   <li>{@link com.mehmandarov.confapi.gatekeepers.NoProfanity} — custom Bean Validation constraint</li>
 * </ul>
 */
package com.mehmandarov.confapi.gatekeepers;

