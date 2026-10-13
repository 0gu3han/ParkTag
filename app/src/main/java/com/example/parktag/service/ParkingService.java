package com.example.parktag.service;

import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;

import java.util.List;

public class ParkingService {

    private final ParkingRepository repository;

    public ParkingService(ParkingRepository repository) {
        this.repository = repository;
    }

    public long saveSession(ParkingSession session) {
        validateSession(session);

        if (session.getId() != 0) {
            throw new IllegalArgumentException("This session has already been saved");
        }

        long now = System.currentTimeMillis();

        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        return repository.save(session);
    }

    public ParkingSession getSession(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }

        return repository.getById(id);
    }

    public List<ParkingSession> getHistory() {
        return repository.getHistory();
    }

    private void validateSession(ParkingSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Parking session is required");
        }

        double latitude = session.getLatitude();
        double longitude = session.getLongitude();

        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        Float accuracy = session.getAccuracyMeters();

        if (accuracy != null && (!Float.isFinite(accuracy) || accuracy < 0)) {
            throw new IllegalArgumentException("Accuracy must be a non-negative number");
        }

        if (session.getCapturedAt() <= 0) {
            throw new IllegalArgumentException("A location capture time is required");
        }

        String note = session.getNote();

        if (note != null && note.codePointCount(0, note.length()) > 280) {
            throw new IllegalArgumentException("Note must be less than 280 characters");
        }

        if (session.getClosedAt() != null) {
            throw new IllegalArgumentException("A new parking session must be open");
        }

        if (session.getReminderAt() != null) {
            throw new IllegalArgumentException("Reminder scheduling has not been implemented yet");
        }
    }

    public boolean closeSession(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }

        return repository.closeSession(id, System.currentTimeMillis());
    }
}