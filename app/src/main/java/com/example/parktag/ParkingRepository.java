package com.example.parktag;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParkingRepository {

    private final ParkingLocationDao dao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public ParkingRepository(Context context) {
        ParkTagDatabase db = ParkTagDatabase.getInstance(context);
        this.dao = db.parkingLocationDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // Save a new parking location
    public void saveParkingLocation(double latitude, double longitude, String notes) {
        String timestamp = java.text.DateFormat
                .getDateTimeInstance()
                .format(new java.util.Date());

        ParkingLocation location = new ParkingLocation(latitude, longitude, timestamp, notes);

        executor.execute(() -> dao.insert(location));
    }

    // Get the most recently saved parking location
    public void getLatestParkingLocation(Callback<ParkingLocation> callback) {
        executor.execute(() -> {
            ParkingLocation location = dao.getLatest();
            mainHandler.post(() -> callback.onResult(location));
        });
    }

    // Get full parking history
    public void getParkingHistory(Callback<List<ParkingLocation>> callback) {
        executor.execute(() -> {
            List<ParkingLocation> history = dao.getAll();
            mainHandler.post(() -> callback.onResult(history));
        });
    }

    // Delete a specific parking location
    public void deleteParkingLocation(ParkingLocation location) {
        executor.execute(() -> dao.delete(location));
    }

    // Delete all parking locations
    public void deleteAllParkingLocations() {
        executor.execute(dao::deleteAll);
    }

    // Simple callback interface for returning results to the UI thread
    public interface Callback<T> {
        void onResult(T result);
    }
}
