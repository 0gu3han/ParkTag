package com.example.parktag;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE =
            "com.example.parktag.LATITUDE";
    public static final String EXTRA_LONGITUDE =
            "com.example.parktag.LONGITUDE";
    public static final String EXTRA_ACCURACY =
            "com.example.parktag.ACCURACY";
    public static final String EXTRA_CAPTURED_AT =
            "com.example.parktag.CAPTURED_AT";

    private static final String STATE_LOCATION = "selected_location";

    private GoogleMap map;
    private TextView status;
    private Button currentLocationButton;
    private Button useLocationButton;

    private FusedLocationProviderClient locationClient;
    private CancellationTokenSource pendingRequest;
    private Location selectedLocation;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                                || hasPermission(
                                Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            locate();
                        } else {
                            status.setText(R.string.picker_permission_denied);
                        }
                    }
            );

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_picker);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.locationPickerRoot),
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            bars.left, bars.top, bars.right, bars.bottom
                    );
                    return windowInsets;
                }
        );

        MaterialToolbar toolbar = findViewById(R.id.toolbarPicker);
        toolbar.setNavigationOnClickListener(view -> finish());

        status = findViewById(R.id.tvLocationStatus);
        currentLocationButton = findViewById(R.id.btnCurrentLocation);
        useLocationButton = findViewById(R.id.btnUseLocation);

        locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (savedInstanceState != null) {
            selectedLocation =
                    savedInstanceState.getParcelable(STATE_LOCATION);
        }

        currentLocationButton.setOnClickListener(
                view -> requestLocationAccess()
        );

        useLocationButton.setOnClickListener(view -> continueToSave());

        SupportMapFragment fragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.locationPickerMap);

        if (fragment == null) {
            status.setText(R.string.picker_map_unavailable);
            return;
        }

        fragment.getMapAsync(googleMap -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);
            currentLocationButton.setEnabled(true);

            map.setOnMapLongClickListener(point -> {
                cancelPendingRequest();

                Location manualLocation = new Location("manual");
                manualLocation.setLatitude(point.latitude);
                manualLocation.setLongitude(point.longitude);
                manualLocation.setTime(System.currentTimeMillis());

                // Manual selection has no measured GPS accuracy.
                selectLocation(manualLocation);
            });

            if (selectedLocation != null) {
                selectLocation(selectedLocation);
            } else {
                map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                new LatLng(20.0, 0.0),
                                2.0f
                        )
                );

                // The user entered this screen by tapping Save parking.
                // On recreation, let them explicitly retry if necessary.
                if (savedInstanceState == null) {
                    requestLocationAccess();
                }
            }
        });
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationAccess() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            locate();
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

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.picker_rationale_title)
                    .setMessage(R.string.picker_rationale_body)
                    .setPositiveButton(
                            R.string.picker_rationale_continue,
                            (dialog, which) ->
                                    permissionLauncher.launch(permissions)
                    )
                    .setNegativeButton(
                            R.string.picker_rationale_manual,
                            (dialog, which) -> status.setText(R.string.picker_long_press)
                    )
                    .show();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    private void locate() {
        boolean precise =
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean approximate =
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (!precise && !approximate) {
            status.setText(R.string.picker_unavailable);
            return;
        }

        cancelPendingRequest();

        selectedLocation = null;
        useLocationButton.setEnabled(false);
        map.clear();

        status.setText(R.string.picker_finding);

        CancellationTokenSource request = new CancellationTokenSource();
        pendingRequest = request;

        CurrentLocationRequest locationRequest =
                new CurrentLocationRequest.Builder()
                        .setPriority(
                                precise
                                        ? Priority.PRIORITY_HIGH_ACCURACY
                                        : Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        )
                        .setMaxUpdateAgeMillis(10000L)
                        .setDurationMillis(15000L)
                        .build();

        try {
            locationClient.getCurrentLocation(
                    locationRequest,
                    request.getToken()
            ).addOnSuccessListener(location -> {
                if (!isCurrentRequest(request)) {
                    return;
                }

                pendingRequest = null;

                if (location == null) {
                    status.setText(R.string.picker_not_found);
                    return;
                }

                if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Approximate location only helps center the map.
                    // Require an explicit manual pin before continuing.
                    drawLocation(location, 13.0f);

                    status.setText(R.string.picker_approximate);
                    return;
                }

                selectLocation(location);

            }).addOnFailureListener(error -> {
                if (!isCurrentRequest(request)) {
                    return;
                }

                pendingRequest = null;

                android.util.Log.e(
                        "ParkTag",
                        "Location capture failed",
                        error
                );

                status.setText(R.string.picker_failed);
            });

        } catch (SecurityException error) {
            cancelPendingRequest();

            status.setText(R.string.picker_permission_changed);
        }
    }

    private boolean isCurrentRequest(CancellationTokenSource request) {
        return pendingRequest == request
                && !isFinishing()
                && !isDestroyed();
    }

    private void selectLocation(Location location) {
        selectedLocation = new Location(location);

        drawLocation(selectedLocation, 18.0f);
        useLocationButton.setEnabled(true);

        if ("manual".equals(selectedLocation.getProvider())) {
            status.setText(R.string.picker_manual_selected);
        } else if (selectedLocation.hasAccuracy()) {
            status.setText(getString(
                    R.string.picker_selected_accuracy,
                    selectedLocation.getAccuracy()
            ));
        } else {
            status.setText(R.string.picker_selected_no_accuracy);
        }
    }

    private void drawLocation(Location location, float zoom) {
        LatLng point = new LatLng(
                location.getLatitude(),
                location.getLongitude()
        );

        map.clear();

        map.addMarker(
                new MarkerOptions()
                        .position(point)
                        .title(getString(R.string.picker_marker_title))
        );

        if (location.hasAccuracy()) {
            int brand = getColor(R.color.brand_primary);
            map.addCircle(
                    new CircleOptions()
                            .center(point)
                            .radius(location.getAccuracy())
                            .strokeColor(brand)
                            .fillColor((brand & 0x00FFFFFF) | 0x22000000)
                            .strokeWidth(2.0f)
            );
        }

        map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(point, zoom)
        );
    }

    private void continueToSave() {
        if (selectedLocation == null) {
            return;
        }

        cancelPendingRequest();
        useLocationButton.setEnabled(false);

        Intent intent = new Intent(this, SaveParkingActivity.class);
        intent.putExtra(EXTRA_LATITUDE, selectedLocation.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, selectedLocation.getLongitude());
        intent.putExtra(EXTRA_CAPTURED_AT, selectedLocation.getTime());

        if (selectedLocation.hasAccuracy()) {
            intent.putExtra(EXTRA_ACCURACY, selectedLocation.getAccuracy());
        }

        startActivity(intent);
        finish();
    }

    private void cancelPendingRequest() {
        CancellationTokenSource request = pendingRequest;
        pendingRequest = null;

        if (request != null) {
            request.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (selectedLocation != null) {
            outState.putParcelable(STATE_LOCATION, selectedLocation);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        if (pendingRequest != null) {
            cancelPendingRequest();

            status.setText(R.string.picker_stopped);
        }

        super.onStop();
    }
}
