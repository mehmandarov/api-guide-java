/**
 * <strong>Pattern 5: The Evolution</strong>
 * <p>
 * Battle-tested strategies for API versioning that don't hurt — supporting
 * both URI-based and header-based versioning simultaneously.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link com.mehmandarov.confapi.versioning.HeaderVersionFilter} — {@code @PreMatching} URI rewriter</li>
 * </ul>
 *
 * @see com.mehmandarov.confapi.resource.v1 V1 resources (flat DTOs)
 * @see com.mehmandarov.confapi.resource.v2 V2 resources (enriched DTOs)
 */
package com.mehmandarov.confapi.versioning;

