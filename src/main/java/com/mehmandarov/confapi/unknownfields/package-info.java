/**
 * Demo for unknown / extra JSON properties during deserialization.
 * <p>
 * Companion code for the blog post "Extra fields, ignored or not: unknown JSON
 * properties in the MicroProfile REST Client".
 * {@link com.mehmandarov.confapi.unknownfields.UnknownFieldsResource} returns a
 * payload with more fields than
 * {@link com.mehmandarov.confapi.unknownfields.Room} declares;
 * {@code Ch7_UnknownFieldsTest} shows how JSON-B and Jackson differ on what
 * happens next.
 */
package com.mehmandarov.confapi.unknownfields;

