package com.example.parktag;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_save_parking);

        // Coordinates come from LocationPickerActivity; without them there is nothing to save
        if (!getIntent().hasExtra(LocationPickerActivity.EXTRA_LATITUDE)
                || !getIntent().hasExtra(LocationPickerActivity.EXTRA_LONGITUDE)
                || !getIntent().hasExtra(LocationPickerActivity.EXTRA_CAPTURED_AT)) {
            Toast.makeText(this, R.string.error_choose_location_first, Toast.LENGTH_LONG).show();
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

        MaterialToolbar toolbar = findViewById(R.id.toolbarSave);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvLocationHint = findViewById(R.id.tvLocationHint);
        if (getIntent().hasExtra(LocationPickerActivity.EXTRA_ACCURACY)) {
            float accuracy = getIntent().getFloatExtra(LocationPickerActivity.EXTRA_ACCURACY, 0f);
            tvLocationHint.setText(getString(R.string.label_location_accuracy, Math.round(accuracy)));
        } else {
            tvLocationHint.setText(R.string.label_location_manual);
        }

        findViewById(R.id.btnConfirmSave).setOnClickListener(v -> saveParking());
    }

    private void saveParking() {
        double latitude = getIntent().getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, Double.NaN);
        double longitude = getIntent().getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, Double.NaN);
        long capturedAt = getIntent().getLongExtra(LocationPickerActivity.EXTRA_CAPTURED_AT, 0L);

        // Manually pinned spots have no measured GPS accuracy
        Float accuracyMeters = null;
        if (getIntent().hasExtra(LocationPickerActivity.EXTRA_ACCURACY)) {
            accuracyMeters = getIntent().getFloatExtra(LocationPickerActivity.EXTRA_ACCURACY, Float.NaN);
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
                    Log.e("ParkTag", "Failed to save parking session", error);
                    Toast.makeText(this, R.string.error_save_session, Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
