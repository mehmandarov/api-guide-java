/**
 * <strong>Pattern 4: The Living Contract</strong>
 * <p>
 * Treating OpenAPI as the single source of truth — the spec is generated
 * from code, never maintained separately.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link com.mehmandarov.confapi.openapi.ConferenceOASFilter} — programmatic OpenAPI enrichment</li>
 * </ul>
 *
 * @see com.mehmandarov.confapi.ApiApplication for {@code @OpenAPIDefinition}
 */
package com.mehmandarov.confapi.openapi;

