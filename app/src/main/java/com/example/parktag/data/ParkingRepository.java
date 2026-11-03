package com.example.parktag.data;

import com.example.parktag.model.ParkingSession;

import java.util.List;

public class ParkingRepository {

    private final ParkingSessionDao dao;

    public ParkingRepository(ParkingSessionDao dao) {
        this.dao = dao;
    }

    public long save(ParkingSession session) {
        long generatedId = dao.insert(session);
        session.setId(generatedId);
        return generatedId;
    }

    public ParkingSession getById(long id) {
        return dao.getById(id);
    }

    public List<ParkingSession> getHistory() {
        return dao.getNewestFirst();
    }

    public boolean closeSession(long id, long timeMillis) {
        return dao.closeSession(id, timeMillis) == 1;
    }
}
