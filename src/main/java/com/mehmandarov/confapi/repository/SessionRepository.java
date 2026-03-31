package com.mehmandarov.confapi.repository;

import com.mehmandarov.confapi.domain.Session;
import com.mehmandarov.confapi.domain.SessionLevel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store. Pre-loaded with sample data for demos.
 */
@ApplicationScoped
public class SessionRepository {

    private final Map<String, Session> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        seed("Bulletproof APIs with Java",
             "Move beyond basic CRUD to explore production-grade API patterns.",
             SessionLevel.INTERMEDIATE, "Backend & APIs", "spk-duke", "room-hall-a",
             LocalDateTime.of(2026, 10, 15, 9, 0), 50);

        seed("Security and Predictability in LLM-Powered Applications",
             "Hands-on workshop on building secure and reliable AI-driven services.",
             SessionLevel.ADVANCED, "Backend & APIs", "spk-jane", "room-hall-b",
             LocalDateTime.of(2026, 10, 15, 10, 0), 90);

        seed("Getting Started with Jakarta EE 11",
             "A beginner-friendly tour of the latest Jakarta EE features.",
             SessionLevel.BEGINNER, "Getting Started", "spk-duke", "room-hall-c",
             LocalDateTime.of(2026, 10, 15, 14, 0), 50);
    }

    private void seed(String title, String abs, SessionLevel level, String track,
                      String speakerId, String roomId, LocalDateTime start, int duration) {
        Session s = new Session();
        s.setTitle(title);
        s.setSessionAbstract(abs);
        s.setLevel(level);
        s.setTrack(track);
        s.setSpeakerId(speakerId);
        s.setRoomId(roomId);
        s.setStartTime(start);
        s.setDurationMinutes(duration);
        store.put(s.getId(), s);
    }

    public List<Session> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Session> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Session save(Session session) {
        store.put(session.getId(), session);
        return session;
    }

    public Optional<Session> delete(String id) {
        return Optional.ofNullable(store.remove(id));
    }

    public boolean exists(String id) {
        return store.containsKey(id);
    }
}

