package com.example.parktag;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ParkingLocationDao {

    @Insert
    void insert(ParkingLocation location);

    @Update
    void update(ParkingLocation location);

    @Delete
    void delete(ParkingLocation location);

    @Query("SELECT * FROM parking_locations ORDER BY id DESC LIMIT 1")
    ParkingLocation getLatest();

    @Query("SELECT * FROM parking_locations ORDER BY id DESC")
    List<ParkingLocation> getAll();

    @Query("DELETE FROM parking_locations")
    void deleteAll();
}
