package com.example.parktag.data;

import android.os.Handler;
import android.os.Looper;

import com.example.parktag.model.ParkingSession;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParkingRepository {

    private static final ExecutorService DATABASE_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final ParkingSessionDao dao;
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T result, Exception error);
    }

    public ParkingRepository(ParkingSessionDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    private <T> void execute(
            Callable<T> operation,
            Callback<T> callback
    ) {
        Objects.requireNonNull(callback);

        DATABASE_EXECUTOR.execute(() -> {
            final T result;

            try {
                result = operation.call();
            } catch (Exception error) {
                mainHandler.post(() ->
                        callback.onResult(null, error));
                return;
            }

            mainHandler.post(() ->
                    callback.onResult(result, null));
        });
    }

    public void save(ParkingSession session, Callback<Long> callback) {
        execute(() -> {
            long generatedId = dao.insert(session);
            session.setId(generatedId);
            return generatedId;
        }, callback);
    }

    public void getById(long id, Callback<ParkingSession> callback) {
        execute(() -> dao.getById(id), callback);
    }

    public void getHistory(Callback<List<ParkingSession>> callback) {
        execute(dao::getNewestFirst, callback);
    }

    public void closeSession(long id, Callback<Boolean> callback) {
        execute(() -> dao.closeSession(id, System.currentTimeMillis()) > 0, callback);
    }

    public void update(ParkingSession session, Callback<Void> callback) {
        execute(() -> {
            session.setUpdatedAt(System.currentTimeMillis());
            dao.update(session);
            return null;
        }, callback);
    }
}
