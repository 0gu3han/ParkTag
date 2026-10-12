package com.example.parktag;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ParkingLocation.class}, version = 1, exportSchema = false)
public abstract class ParkTagDatabase extends RoomDatabase {

    private static ParkTagDatabase instance;

    public abstract ParkingLocationDao parkingLocationDao();

    public static synchronized ParkTagDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    ParkTagDatabase.class,
                    "parktag_database"
            ).build();
        }
        return instance;
    }
}
