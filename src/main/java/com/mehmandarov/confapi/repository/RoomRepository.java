package com.mehmandarov.confapi.repository;

import com.mehmandarov.confapi.domain.Room;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory room store.
 */
@ApplicationScoped
public class RoomRepository {

    private final Map<String, Room> store = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        seed("room-hall-a", "Hall A", 300, "Convention Center");
        seed("room-hall-b", "Hall B", 150, "Convention Center");
        seed("room-hall-c", "Room C", 60,  "East Wing");
    }

    private void seed(String id, String name, int capacity, String building) {
        Room r = new Room();
        r.setId(id);
        r.setName(name);
        r.setCapacity(capacity);
        r.setBuilding(building);
        store.put(r.getId(), r);
    }

    public List<Room> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Room> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}

