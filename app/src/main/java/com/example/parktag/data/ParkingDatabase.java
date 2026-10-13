package com.example.parktag.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.parktag.model.ParkingSession;

@Database(
        entities = {ParkingSession.class},
        version = 1,
        exportSchema = true
)
public abstract class ParkingDatabase extends RoomDatabase {

    private static volatile ParkingDatabase instance;

    public abstract ParkingSessionDao parkingSessionDao();

    public static ParkingDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ParkingDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ParkingDatabase.class,
                            "parktag.db"
                    ).build();
                }
            }
        }

        return instance;
    }
}