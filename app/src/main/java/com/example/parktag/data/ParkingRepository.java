package com.example.parktag.data;

import android.os.Handler;
import android.os.Looper;

import com.example.parktag.model.ParkingSession;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParkingRepository {

    private final ParkingSessionDao dao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onResult(T result);
    }

    public ParkingRepository(ParkingSessionDao dao) {
        this.dao = dao;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void save(ParkingSession session, Callback<Long> callback) {
        executor.execute(() -> {
            long generatedId = dao.insert(session);
            session.setId(generatedId);
            mainHandler.post(() -> callback.onResult(generatedId));
        });
    }

    public void getById(long id, Callback<ParkingSession> callback) {
        executor.execute(() -> {
            ParkingSession session = dao.getById(id);
            mainHandler.post(() -> callback.onResult(session));
        });
    }

    public void getHistory(Callback<List<ParkingSession>> callback) {
        executor.execute(() -> {
            List<ParkingSession> history = dao.getNewestFirst();
            mainHandler.post(() -> callback.onResult(history));
        });
    }

    public void closeSession(long id, Callback<Boolean> callback) {
        executor.execute(() -> {
            boolean success = dao.closeSession(id, System.currentTimeMillis()) == 1;
            mainHandler.post(() -> callback.onResult(success));
        });
    }

    public void update(ParkingSession session, Callback<Void> callback) {
        executor.execute(() -> {
            session.setUpdatedAt(System.currentTimeMillis());
            dao.update(session);
            mainHandler.post(() -> callback.onResult(null));
        });
    }
}
