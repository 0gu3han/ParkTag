package com.example.parktag;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;

public class MainActivity extends AppCompatActivity {

    private ParkingService parkingService;
    private ParkingSession currentSession;

    private TextView tvNoParkingMessage;
    private TextView tvSavedAt;
    private TextView tvLevel;
    private TextView tvSection;
    private TextView tvNotes;
    private Button btnViewMap;
    private Button btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        parkingService = new ParkingService(repository);

        tvNoParkingMessage = findViewById(R.id.tvNoParkingMessage);
        tvSavedAt = findViewById(R.id.tvSavedAt);
        tvLevel = findViewById(R.id.tvLevel);
        tvSection = findViewById(R.id.tvSection);
        tvNotes = findViewById(R.id.tvNotes);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnClear = findViewById(R.id.btnClear);

        findViewById(R.id.btnSaveParking).setOnClickListener(v -> onSaveParkingClicked());
        btnViewMap.setOnClickListener(v -> openMap());
        btnClear.setOnClickListener(v -> confirmClear());
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        loadLatestSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLatestSession();
    }

    private void loadLatestSession() {
        parkingService.getHistory((sessions, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                android.util.Log.e("ParkTag", "Failed to load parking session", error);

                android.widget.Toast.makeText(
                        this,
                        "Could not load your parking location. Please try again.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

                return;
            }

            ParkingSession latest = null;

            if (sessions != null) {
                for (ParkingSession session : sessions) {
                    if (session.getClosedAt() == null) {
                        latest = session;
                        break;
                    }
                }
            }

            currentSession = latest;
            updateUI();
        });
    }

    private void updateUI() {
        if (currentSession == null) {
            tvNoParkingMessage.setVisibility(View.VISIBLE);
            tvSavedAt.setVisibility(View.GONE);
            tvLevel.setVisibility(View.GONE);
            tvSection.setVisibility(View.GONE);
            tvNotes.setVisibility(View.GONE);
            btnViewMap.setEnabled(false);
            btnClear.setEnabled(false);
        } else {
            tvNoParkingMessage.setVisibility(View.GONE);

            tvSavedAt.setText(getString(R.string.label_saved_at,
                    new java.util.Date(currentSession.getCreatedAt()).toString()));
            tvSavedAt.setVisibility(View.VISIBLE);

            if (currentSession.getLevel() != null && !currentSession.getLevel().isEmpty()) {
                tvLevel.setText(getString(R.string.label_level, currentSession.getLevel()));
                tvLevel.setVisibility(View.VISIBLE);
            }

            if (currentSession.getSection() != null && !currentSession.getSection().isEmpty()) {
                tvSection.setText(getString(R.string.label_section, currentSession.getSection()));
                tvSection.setVisibility(View.VISIBLE);
            }

            String note = currentSession.getNote();
            tvNotes.setText(getString(R.string.label_notes,
                    (note != null && !note.isEmpty()) ? note : getString(R.string.label_no_notes)));
            tvNotes.setVisibility(View.VISIBLE);

            btnViewMap.setEnabled(true);
            btnClear.setEnabled(true);
        }
    }

    private void onSaveParkingClicked() {
        // Will's GPS component will provide coordinates here.
        // For now, launch SaveParkingActivity where user can enter notes/level/section.
        startActivity(new Intent(this, SaveParkingActivity.class));
    }

    private void openMap() {
        if (currentSession != null) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("session_id", currentSession.getId());
            startActivity(intent);
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_clear)
                .setPositiveButton(R.string.yes, (dialog, which) -> clearCurrentSession())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void clearCurrentSession() {
        if (currentSession == null) {
            return;
        }

        parkingService.closeSession(currentSession.getId(), (success, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                android.util.Log.e("ParkTag", "Failed to close parking session", error);

                android.widget.Toast.makeText(
                        this,
                        "Could not close your parking session. Please try again.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (!Boolean.TRUE.equals(success)) {
                android.widget.Toast.makeText(
                        this,
                        "This parking session is already closed or unavailable.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

                loadLatestSession();
                return;
            }

            currentSession = null;
            updateUI();
        });
    }
}