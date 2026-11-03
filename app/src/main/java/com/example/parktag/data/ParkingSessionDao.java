package com.example.parktag.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.parktag.model.ParkingSession;

import java.util.List;

@Dao
public interface ParkingSessionDao {

    @Insert
    long insert(ParkingSession session);

    @Query("SELECT * FROM parking_session WHERE id = :id")
    ParkingSession getById(long id);

    @Query("SELECT * FROM parking_session ORDER BY createdAt DESC, id DESC")
    List<ParkingSession> getNewestFirst();

    @Query("UPDATE parking_session SET closedAt = :timeMillis, updatedAt = :timeMillis," +
            " reminderAt = NULL WHERE id = :id AND closedAt IS NULL")
    int closeSession(long id, long timeMillis);
}