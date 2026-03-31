/**
 * <strong>Bonus: Sane Error Handling</strong>
 * <p>
 * RFC 9457 Problem Details for HTTP APIs — a single, consistent error
 * envelope that keeps your clients happy.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link com.mehmandarov.confapi.error.ProblemDetail} — RFC 9457 POJO</li>
 *   <li>{@link com.mehmandarov.confapi.error.ConstraintViolationExceptionMapper} — validation → 400</li>
 *   <li>{@link com.mehmandarov.confapi.error.NotFoundExceptionMapper} — 404</li>
 *   <li>{@link com.mehmandarov.confapi.error.CatchAllExceptionMapper} — 500 (no stack leaks)</li>
 * </ul>
 */
package com.mehmandarov.confapi.error;

