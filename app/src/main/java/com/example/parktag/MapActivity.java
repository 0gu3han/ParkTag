package com.example.parktag;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;

/**
 * MapActivity is responsible for displaying the saved parking location on Google Maps.
 * GPS, map markers, distance calculation, and directions are handled by Will.
 * This activity retrieves the saved ParkingSession from the database and passes
 * the coordinates to the map component.
 */
public class MapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        long sessionId = getIntent().getLongExtra("session_id", -1);

        if (sessionId <= 0) {
            android.widget.Toast.makeText(
                    this,
                    "This parking record is unavailable.",
                    android.widget.Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        ParkingService parkingService = new ParkingService(repository);

        parkingService.getSession(sessionId, (session, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                android.util.Log.e("ParkTag", "Failed to load map record", error);

                android.widget.Toast.makeText(
                        this,
                        "Could not load your parking location. Please try again.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

                finish();
                return;
            }

            if (session == null) {
                android.widget.Toast.makeText(
                        this,
                        "This parking record is no longer available.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

                finish();
                return;
            }

            onSessionLoaded(session);
        });
    }

    private void onSessionLoaded(ParkingSession session) {
        double latitude = session.getLatitude();
        double longitude = session.getLongitude();

        // Will's Google Maps integration goes here.
        // Use latitude and longitude to place a marker and show directions.
    }
}
