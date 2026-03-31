package com.mehmandarov.confapi.repository;

import com.mehmandarov.confapi.domain.Speaker;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory speaker store.
 */
@ApplicationScoped
public class SpeakerRepository {

    private final Map<String, Speaker> store = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        seed("spk-duke", "Duke Java", "The original Java mascot, now a seasoned architect.",
             "OpenJDK Foundation", "https://example.com/duke.png");

        seed("spk-jane", "Jane Coder", "Reactive systems expert and open-source contributor.",
             "MicroStream Inc.", "https://example.com/jane.png");
    }

    private void seed(String id, String name, String bio, String company, String photoUrl) {
        Speaker s = new Speaker();
        s.setId(id);
        s.setName(name);
        s.setBio(bio);
        s.setCompany(company);
        s.setPhotoUrl(photoUrl);
        store.put(s.getId(), s);
    }

    public List<Speaker> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Speaker> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Speaker save(Speaker speaker) {
        store.put(speaker.getId(), speaker);
        return speaker;
    }

    public Optional<Speaker> delete(String id) {
        return Optional.ofNullable(store.remove(id));
    }
}

