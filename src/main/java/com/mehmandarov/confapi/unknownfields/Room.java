package com.mehmandarov.confapi.unknownfields;

/**
 * A deliberately minimal "consumer" view of a room.
 * <p>
 * The demo endpoint ({@link UnknownFieldsResource}) returns six fields, but a
 * client that only declares these three should still be able to deserialize the
 * response - provided its JSON binding ignores unknown properties. See
 * {@code Ch7_UnknownFieldsTest} for the proof across JSON-B and Jackson.
 */
public record Room(String id, String name, int capacity) { }

