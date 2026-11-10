package com.example.parktag;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * MapActivity is responsible for displaying the saved parking location on Google Maps.
 * GPS, map markers, distance calculation, and directions are handled by Will.
 * This activity retrieves the saved ParkingSession from the database and passes
 * the coordinates to the map component.
 */
public class MapActivity extends AppCompatActivity {

    private GoogleMap map;
    private ParkingSession savedSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.savedMapRoot),
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return windowInsets;
                }
        );

        long sessionId = getIntent().getLongExtra("session_id", -1L);

        if (sessionId <= 0) {
            showErrorAndFinish(R.string.saved_map_record_missing);
            return;
        }

        SupportMapFragment fragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.savedParkingMap);

        if (fragment == null) {
            showErrorAndFinish(R.string.saved_map_unavailable);
            return;
        }

        fragment.getMapAsync(googleMap -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);

            displaySavedLocation();
        });

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        ParkingService parkingService = new ParkingService(repository);

        parkingService.getSession(sessionId, (session, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                Log.e("ParkTag", "Failed to load map record", error);
                showErrorAndFinish(R.string.saved_map_load_failed);
                return;
            }

            if (session == null) {
                showErrorAndFinish(R.string.saved_map_record_missing);
                return;
            }

            savedSession = session;
            displaySavedLocation();
        });
    }

    private void displaySavedLocation() {
        // The database and map load independently.
        // Draw only after both are ready.
        if (map == null || savedSession == null
                || isFinishing() || isDestroyed()) {
            return;
        }

        LatLng parkingSpot = new LatLng(
                savedSession.getLatitude(),
                savedSession.getLongitude()
        );

        map.clear();

        Marker marker = map.addMarker(
                new MarkerOptions()
                        .position(parkingSpot)
                        .title(getString(R.string.saved_map_marker))
        );

        Float accuracy = savedSession.getAccuracyMeters();

        if (accuracy != null
                && !Float.isNaN(accuracy)
                && !Float.isInfinite(accuracy)
                && accuracy > 0) {
            map.addCircle(
                    new CircleOptions()
                            .center(parkingSpot)
                            .radius(accuracy)
                            .strokeColor(0xFF1565C0)
                            .fillColor(0x221565C0)
                            .strokeWidth(2.0f)
            );
        }

        map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(parkingSpot, 18.0f)
        );

        if (marker != null) {
            marker.showInfoWindow();
        }
    }

    private void showErrorAndFinish(@StringRes int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }
}