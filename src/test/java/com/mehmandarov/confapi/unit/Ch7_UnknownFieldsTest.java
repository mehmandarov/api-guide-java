package com.mehmandarov.confapi.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mehmandarov.confapi.unknownfields.Room;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <strong>Bonus: Unknown JSON fields</strong>
 * <p>
 * Companion to the blog post "Extra fields, ignored or not: unknown JSON
 * properties in the MicroProfile REST Client". These tests exercise the JSON
 * binding layer directly - the same layer the MicroProfile REST Client delegates
 * to - to show that the behaviour on an unknown field depends entirely on the
 * provider:
 * <ul>
 *   <li>JSON-B / Yasson ignores unknown properties by default (per the JSON-B
 *       spec).</li>
 *   <li>Stock Jackson fails by default ({@code FAIL_ON_UNKNOWN_PROPERTIES} is
 *       enabled), and only ignores them once you disable that feature or annotate
 *       the type with {@code @JsonIgnoreProperties(ignoreUnknown = true)}.</li>
 * </ul>
 * Yasson and jackson-databind are pulled in at <em>test scope only</em>; the app
 * itself uses whatever JSON-B implementation the runtime provides.
 */
@DisplayName("Bonus - Unknown JSON fields (provider defaults)")
class Ch7_UnknownFieldsTest {

    /** The six-field payload the demo endpoint returns. {@link Room} declares three. */
    private static final String OVER_STUFFED_ROOM = """
            {
              "id": "room-7",
              "name": "Hall A",
              "capacity": 120,
              "building": "Main",
              "floor": 2,
              "accessibility": { "wheelchair": true }
            }
            """;

    @Nested
    @DisplayName("JSON-B / Yasson - lenient by default")
    class JsonB {

        @Test
        @DisplayName("Ignores unknown fields and maps the rest (no exception)")
        void ignoresUnknownFields() {
            try (Jsonb jsonb = JsonbBuilder.create()) {
                Room room = jsonb.fromJson(OVER_STUFFED_ROOM, Room.class);

                assertAll(
                        () -> assertEquals("room-7", room.id()),
                        () -> assertEquals("Hall A", room.name()),
                        () -> assertEquals(120, room.capacity()));
            } catch (Exception e) {
                fail("JSON-B should ignore unknown fields, but threw: " + e);
            }
        }
    }

    @Nested
    @DisplayName("Jackson - strict by default")
    class Jackson {

        @Test
        @DisplayName("Stock ObjectMapper throws UnrecognizedPropertyException on an unknown field")
        void stockJacksonFails() {
            JsonMapper mapper = JsonMapper.builder().build(); // FAIL_ON_UNKNOWN_PROPERTIES is on

            assertThrows(UnrecognizedPropertyException.class,
                    () -> mapper.readValue(OVER_STUFFED_ROOM, Room.class),
                    "Stock Jackson treats an unknown property as an error");
        }

        @Test
        @DisplayName("Disabling FAIL_ON_UNKNOWN_PROPERTIES makes it lenient")
        void disablingTheFeatureMakesItLenient() throws Exception {
            JsonMapper mapper = JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            Room room = mapper.readValue(OVER_STUFFED_ROOM, Room.class);
            assertEquals("room-7", room.id());
            assertEquals(120, room.capacity());
        }

        @Test
        @DisplayName("@JsonIgnoreProperties(ignoreUnknown = true) makes a single type lenient")
        void annotationMakesOneTypeLenient() throws Exception {
            JsonMapper mapper = JsonMapper.builder().build(); // still strict globally

            RoomLenient room = mapper.readValue(OVER_STUFFED_ROOM, RoomLenient.class);
            assertEquals("room-7", room.id());
            assertEquals(120, room.capacity());
        }
    }

    /** Same three fields as {@link Room}, but opted into lenient Jackson handling. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomLenient(String id, String name, int capacity) { }
}

