package com.example.parktag;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.CancellationTokenSource;

/**
 * MapActivity is responsible for displaying the saved parking location on Google Maps.
 * GPS, map markers, distance calculation, and directions are handled by Will.
 * This activity retrieves the saved ParkingSession from the database and passes
 * the coordinates to the map component.
 */
public class MapActivity extends AppCompatActivity {

    private GoogleMap map;
    private ParkingSession savedSession;
    private TextView distanceText;
    private Button refreshLocationButton;
    private FusedLocationProviderClient locationClient;
    private CancellationTokenSource pendingLocationRequest;
    private Marker currentLocationMarker;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (hasLocationPermission()) {
                            refreshCurrentLocation();
                        } else {
                            distanceText.setText(
                                    R.string.find_car_permission_denied
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        distanceText = findViewById(R.id.tvDistanceToCar);
        refreshLocationButton = findViewById(R.id.btnRefreshMyLocation);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        refreshLocationButton.setOnClickListener(
                view -> requestCurrentLocation()
        );

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

        refreshLocationButton.setEnabled(true);
    }

    private void showErrorAndFinish(@StringRes int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }

    private boolean hasPreciseLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return hasPreciseLocationPermission()
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCurrentLocation() {
        if (map == null || savedSession == null) {
            return;
        }

        if (hasLocationPermission()) {
            refreshCurrentLocation();
            return;
        }

        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_COARSE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle(R.string.find_car_permission_title)
                    .setMessage(R.string.find_car_permission_explanation)
                    .setPositiveButton(
                            R.string.find_car_continue,
                            (dialog, which) ->
                                    locationPermissionLauncher.launch(permissions)
                    )
                    .setNegativeButton(R.string.find_car_cancel, null)
                    .show();
        } else {
            locationPermissionLauncher.launch(permissions);
        }
    }

    private void refreshCurrentLocation() {
        if (map == null || savedSession == null
                || isFinishing() || isDestroyed()) {
            return;
        }

        cancelLocationRequest();
        clearCurrentLocationMarker();

        if (!hasLocationPermission()) {
            distanceText.setText(R.string.find_car_permission_denied);
            return;
        }

        distanceText.setText(R.string.find_car_loading);
        refreshLocationButton.setEnabled(false);

        CancellationTokenSource request = new CancellationTokenSource();
        pendingLocationRequest = request;

        CurrentLocationRequest locationRequest =
                new CurrentLocationRequest.Builder()
                        .setPriority(
                                hasPreciseLocationPermission()
                                        ? Priority.PRIORITY_HIGH_ACCURACY
                                        : Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        )
                        .setMaxUpdateAgeMillis(5000L)
                        .setDurationMillis(15000L)
                        .build();

        try {
            locationClient.getCurrentLocation(
                    locationRequest,
                    request.getToken()
            ).addOnSuccessListener(location -> {
                if (!isActiveLocationRequest(request)) {
                    return;
                }

                pendingLocationRequest = null;
                refreshLocationButton.setEnabled(true);

                if (!hasLocationPermission()) {
                    distanceText.setText(R.string.find_car_permission_denied);
                    return;
                }

                if (location == null) {
                    distanceText.setText(R.string.find_car_no_location);
                    return;
                }

                showCurrentLocation(location);

            }).addOnFailureListener(error -> {
                if (!isActiveLocationRequest(request)) {
                    return;
                }

                pendingLocationRequest = null;
                refreshLocationButton.setEnabled(true);

                Log.e("ParkTag", "Current location lookup failed", error);
                distanceText.setText(R.string.find_car_location_failed);
            });

        } catch (SecurityException error) {
            cancelLocationRequest();
            distanceText.setText(R.string.find_car_permission_denied);
        }
    }

    private boolean isActiveLocationRequest(
            CancellationTokenSource request
    ) {
        return pendingLocationRequest == request
                && !isFinishing()
                && !isDestroyed();
    }

    private void showCurrentLocation(Location location) {
        LatLng yourPosition = new LatLng(
                location.getLatitude(),
                location.getLongitude()
        );

        LatLng carPosition = new LatLng(
                savedSession.getLatitude(),
                savedSession.getLongitude()
        );

        boolean precise = hasPreciseLocationPermission();

        clearCurrentLocationMarker();

        currentLocationMarker = map.addMarker(
                new MarkerOptions()
                        .position(yourPosition)
                        .title(getString(
                                precise
                                        ? R.string.find_car_your_location
                                        : R.string.find_car_approximate_location
                        ))
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                        ))
        );

        float[] results = new float[1];

        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                savedSession.getLatitude(),
                savedSession.getLongitude(),
                results
        );

        distanceText.setText(getString(
                precise
                        ? R.string.find_car_distance
                        : R.string.find_car_distance_approximate,
                results[0]
        ));

        // Keep both the car and your current position visible.
        View mapView = findViewById(R.id.savedParkingMap);
        int width = mapView.getWidth();
        int height = mapView.getHeight();

        if (results[0] < 1.0f) {
            map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(carPosition, 18.0f)
            );
        } else if (width > 0 && height > 0) {
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(carPosition)
                    .include(yourPosition)
                    .build();

            int desiredPadding = Math.round(
                    48 * getResources().getDisplayMetrics().density
            );
            int padding = Math.min(
                    desiredPadding,
                    Math.min(width, height) / 4
            );

            map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            bounds, width, height, padding
                    )
            );
        }
    }

    private void clearCurrentLocationMarker() {
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
            currentLocationMarker = null;
        }
    }

    private void cancelLocationRequest() {
        CancellationTokenSource request = pendingLocationRequest;
        pendingLocationRequest = null;

        if (request != null) {
            request.cancel();
        }

        refreshLocationButton.setEnabled(
                map != null && savedSession != null
        );
    }

    @Override
    protected void onStop() {
        cancelLocationRequest();
        clearCurrentLocationMarker();
        distanceText.setText(R.string.find_car_hint);
        super.onStop();
    }
}