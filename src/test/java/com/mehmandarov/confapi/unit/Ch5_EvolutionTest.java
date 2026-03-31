package com.mehmandarov.confapi.unit;

import com.mehmandarov.confapi.dto.SessionDtoV1;
import com.mehmandarov.confapi.dto.SessionDtoV2;
import com.mehmandarov.confapi.domain.Room;
import com.mehmandarov.confapi.domain.Session;
import com.mehmandarov.confapi.domain.SessionLevel;
import com.mehmandarov.confapi.domain.Speaker;
import com.mehmandarov.confapi.versioning.HeaderVersionFilter;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <strong>Pattern 5: The Evolution</strong>
 * <p>
 * Tests prove: V1 and V2 DTOs represent different API contracts,
 * and the version detection logic correctly parses headers.
 */
@DisplayName("Ch5 — The Evolution (API Versioning)")
class Ch5_EvolutionTest {

    @Nested
    @DisplayName("V1 DTO — flat, foreign-key based (backward compatible)")
    class V1Flat {

        private Session session;

        @BeforeEach
        void setUp() {
            session = new Session();
            session.setId("sess-1");
            session.setTitle("Bulletproof APIs");
            session.setSessionAbstract("Deep dive.");
            session.setLevel(SessionLevel.INTERMEDIATE);
            session.setTrack("Backend");
            session.setSpeakerId("spk-duke");
            session.setRoomId("room-a");
            session.setStartTime(LocalDateTime.of(2026, 10, 15, 9, 0));
            session.setDurationMinutes(50);
        }

        @Test
        @DisplayName("Maps entity to flat DTO with foreign-key IDs (no embedding)")
        void mapsToFlatDto() {
            SessionDtoV1 dto = SessionDtoV1.from(session);

            assertEquals("sess-1", dto.getId());
            assertEquals("spk-duke", dto.getSpeakerId(), "V1 uses speakerId string, not embedded object");
            assertEquals("room-a", dto.getRoomId(), "V1 uses roomId string, not embedded object");
        }
    }

    @Nested
    @DisplayName("V2 DTO — enriched with embedded speaker & room (breaking change)")
    class V2Enriched {

        private Session session;
        private Speaker speaker;
        private Room room;

        @BeforeEach
        void setUp() {
            session = new Session();
            session.setId("sess-1");
            session.setTitle("Bulletproof APIs");
            session.setSessionAbstract("Deep dive.");
            session.setLevel(SessionLevel.INTERMEDIATE);
            session.setSpeakerId("spk-duke");
            session.setRoomId("room-a");
            session.setDurationMinutes(50);

            speaker = new Speaker();
            speaker.setId("spk-duke");
            speaker.setName("Duke Java");
            speaker.setCompany("OpenJDK");

            room = new Room();
            room.setId("room-a");
            room.setName("Hall A");
            room.setBuilding("Convention Center");
        }

        @Test
        @DisplayName("Embeds full speaker object instead of ID → richer client experience")
        void embedsSpeaker() {
            SessionDtoV2 dto = SessionDtoV2.from(session, speaker, room);

            assertNotNull(dto.getSpeaker());
            assertEquals("Duke Java", dto.getSpeaker().name());
            assertEquals("OpenJDK", dto.getSpeaker().company());
        }

        @Test
        @DisplayName("Embeds full room object instead of ID")
        void embedsRoom() {
            SessionDtoV2 dto = SessionDtoV2.from(session, speaker, room);

            assertNotNull(dto.getRoom());
            assertEquals("Hall A", dto.getRoom().name());
        }

        @Test
        @DisplayName("Handles missing speaker/room gracefully (null-safe)")
        void handlesNulls() {
            SessionDtoV2 dto = SessionDtoV2.from(session, null, null);
            assertNull(dto.getSpeaker());
            assertNull(dto.getRoom());
            assertEquals("sess-1", dto.getId(), "Session fields still present");
        }
    }

    @Nested
    @DisplayName("Version Detection — parsing X-API-Version and Accept headers")
    class VersionDetection {

        private HeaderVersionFilter filter;

        @BeforeEach
        void setUp() {
            filter = new HeaderVersionFilter();
        }

        @Test
        @DisplayName("X-API-Version: 2 → detected as version '2'")
        void detectsXApiVersionHeader() throws Exception {
            String version = invokeDetectVersion("X-API-Version", "2", null);
            assertEquals("2", version);
        }

        @Test
        @DisplayName("X-API-Version: 1 → detected as version '1'")
        void detectsVersionOne() throws Exception {
            String version = invokeDetectVersion("X-API-Version", "1", null);
            assertEquals("1", version);
        }

        @Test
        @DisplayName("Accept: application/json; version=2 → detected as version '2'")
        void detectsAcceptHeaderVersion() throws Exception {
            String version = invokeDetectVersion(null, null, "application/json; version=2");
            assertEquals("2", version);
        }

        @Test
        @DisplayName("No version header → null (defaults to URI-based routing)")
        void noVersionHeaderReturnsNull() throws Exception {
            String version = invokeDetectVersion(null, null, null);
            assertNull(version, "No version header means use URI-based versioning");
        }

        @Test
        @DisplayName("X-API-Version takes priority over Accept header")
        void xApiVersionTakesPriority() throws Exception {
            String version = invokeDetectVersion("X-API-Version", "1", "application/json; version=2");
            assertEquals("1", version,
                    "X-API-Version should win when both are present");
        }

        /**
         * Invokes the private detectVersion method with a stubbed ContainerRequestContext.
         */
        private String invokeDetectVersion(String headerName, String headerValue, String acceptHeader)
                throws Exception {
            Method m = HeaderVersionFilter.class.getDeclaredMethod("detectVersion", ContainerRequestContext.class);
            m.setAccessible(true);

            ContainerRequestContext ctx = new StubRequestContext(headerName, headerValue, acceptHeader);
            return (String) m.invoke(filter, ctx);
        }
    }

    // ── Minimal stub for version detection tests ────────────────

    private static class StubRequestContext implements ContainerRequestContext {
        private final Map<String, String> headers = new HashMap<>();

        StubRequestContext(String headerName, String headerValue, String acceptHeader) {
            if (headerName != null) headers.put(headerName, headerValue);
            if (acceptHeader != null) headers.put("Accept", acceptHeader);
        }

        @Override public String getHeaderString(String name) { return headers.get(name); }

        // --- Unused stubs ---
        @Override public Object getProperty(String name) { return null; }
        @Override public Collection<String> getPropertyNames() { return List.of(); }
        @Override public void setProperty(String name, Object object) { }
        @Override public void removeProperty(String name) { }
        @Override public UriInfo getUriInfo() { return null; }
        @Override public void setRequestUri(URI requestUri) { }
        @Override public void setRequestUri(URI baseUri, URI requestUri) { }
        @Override public jakarta.ws.rs.core.Request getRequest() { return null; }
        @Override public String getMethod() { return "GET"; }
        @Override public void setMethod(String method) { }
        @Override public MultivaluedMap<String, String> getHeaders() { return new MultivaluedHashMap<>(); }
        @Override public List<jakarta.ws.rs.core.MediaType> getAcceptableMediaTypes() { return List.of(); }
        @Override public List<Locale> getAcceptableLanguages() { return List.of(); }
        @Override public jakarta.ws.rs.core.MediaType getMediaType() { return null; }
        @Override public Map<String, jakarta.ws.rs.core.Cookie> getCookies() { return Map.of(); }
        @Override public boolean hasEntity() { return false; }
        @Override public InputStream getEntityStream() { return null; }
        @Override public void setEntityStream(InputStream input) { }
        @Override public SecurityContext getSecurityContext() { return null; }
        @Override public void setSecurityContext(SecurityContext context) { }
        @Override public void abortWith(jakarta.ws.rs.core.Response response) { }
        @Override public Date getDate() { return null; }
        @Override public int getLength() { return 0; }
        @Override public Locale getLanguage() { return null; }

        // Jakarta REST 4.0 (EE 11) additions — no @Override so it compiles with 3.1 too
        public boolean containsHeaderString(String name, String sep, java.util.function.Predicate<String> p) { return false; }
        public boolean containsHeaderString(String name, java.util.function.Predicate<String> p) { return false; }
    }
}

