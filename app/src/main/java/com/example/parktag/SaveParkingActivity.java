package com.example.parktag;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;

public class SaveParkingActivity extends AppCompatActivity {

    private ParkingService parkingService;
    private EditText etLevel;
    private EditText etSection;
    private EditText etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_parking);

        if (!getIntent().hasExtra(LocationPickerActivity.EXTRA_LATITUDE)
                ||
        !getIntent().hasExtra(LocationPickerActivity.EXTRA_LONGITUDE)
                ||
        !getIntent().hasExtra(LocationPickerActivity.EXTRA_CAPTURED_AT)) {

            Toast.makeText(
                    this,
                    "Choose a parking location before saving.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        parkingService = new ParkingService(repository);

        etLevel = findViewById(R.id.etLevel);
        etSection = findViewById(R.id.etSection);
        etNotes = findViewById(R.id.etNotes);

        findViewById(R.id.btnConfirmSave).setOnClickListener(v -> saveParking());
    }

    private void saveParking() {
        // Coordinates will come from Will's GPS component.
        // Placeholder values used until GPS integration is complete.
        double latitude = getIntent().getDoubleExtra(
                LocationPickerActivity.EXTRA_LATITUDE,
                Double.NaN
        );

        double longitude = getIntent().getDoubleExtra(
                LocationPickerActivity.EXTRA_LONGITUDE,
                Double.NaN
        );

        long capturedAt = getIntent().getLongExtra(
                LocationPickerActivity.EXTRA_CAPTURED_AT,
                0L
        );

        Float accuracyMeters = null;

        if (getIntent().hasExtra(LocationPickerActivity.EXTRA_ACCURACY)) {
            accuracyMeters = getIntent().getFloatExtra(
                    LocationPickerActivity.EXTRA_ACCURACY,
                    Float.NaN
            );
        }

        ParkingSession session = new ParkingSession();
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setAccuracyMeters(accuracyMeters);
        session.setCapturedAt(capturedAt);
        session.setLevel(etLevel.getText().toString().trim());
        session.setSection(etSection.getText().toString().trim());
        session.setNote(etNotes.getText().toString().trim());

        try {
            parkingService.saveSession(session, (id, error) -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (error != null) {
                    android.util.Log.e(
                            "ParkTag",
                            "Failed to save parking session",
                            error
                    );

                    Toast.makeText(
                            this,
                            "Could not save your parking location. Please try again.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                Toast.makeText(
                        this,
                        "Parking location saved!",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            });
        } catch (IllegalArgumentException error) {
            Toast.makeText(
                    this,
                    error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
