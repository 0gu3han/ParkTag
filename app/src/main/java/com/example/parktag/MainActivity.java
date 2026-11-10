package com.example.parktag;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ParkTag";

    private static final int GEAR_P = 0;
    private static final int GEAR_R = 1;
    private static final int GEAR_N = 2;
    private static final int GEAR_D = 3;

    // Short pause so the knob visibly lands in P/R before the next screen opens
    private static final long SHIFT_DELAY_MS = 250;

    private int currentGear = GEAR_D;

    private ParkingService parkingService;
    private ParkingSession currentSession;

    private GearShifterView gearShifter;

    // Current spot card views
    private TextView tvStatusPill;
    private TextView tvNoParkingMessage;
    private TextView tvSavedAt;
    private TextView tvSavedAgo;
    private LinearLayout tagsRow;
    private TextView tvLevel;
    private TextView tvSection;
    private TextView tvNotes;
    private LinearLayout actionButtons;
    private Button btnViewMap;
    private Button btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        parkingService = new ParkingService(repository);

        gearShifter = findViewById(R.id.gearShifter);
        gearShifter.setDescriptions(
                getString(R.string.gear_desc_park),
                getString(R.string.gear_desc_reverse),
                getString(R.string.gear_desc_neutral),
                getString(R.string.gear_desc_drive));
        gearShifter.setGear(GEAR_D, false);
        gearShifter.setOnGearChangeListener(this::onGearSelected);

        tvStatusPill = findViewById(R.id.tvStatusPill);
        tvNoParkingMessage = findViewById(R.id.tvNoParkingMessage);
        tvSavedAt = findViewById(R.id.tvSavedAt);
        tvSavedAgo = findViewById(R.id.tvSavedAgo);
        tagsRow = findViewById(R.id.tagsRow);
        tvLevel = findViewById(R.id.tvLevel);
        tvSection = findViewById(R.id.tvSection);
        tvNotes = findViewById(R.id.tvNotes);
        actionButtons = findViewById(R.id.actionButtons);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnClear = findViewById(R.id.btnClear);

        btnViewMap.setOnClickListener(v -> openMap());
        btnClear.setOnClickListener(v -> confirmClear());
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        loadLatestSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Return to D after coming back from P or R
        if (currentGear == GEAR_P || currentGear == GEAR_R) {
            currentGear = GEAR_D;
            gearShifter.setGear(GEAR_D, true);
        }
        loadLatestSession();
    }

    // ── Gear logic ────────────────────────────────────────────────────────────

    private void onGearSelected(int gear) {
        if (gear == currentGear) return;
        currentGear = gear;

        if (gear == GEAR_P) {
            // Pick the location first; LocationPickerActivity then opens the save form
            gearShifter.postDelayed(() ->
                    startActivity(new Intent(this, LocationPickerActivity.class)), SHIFT_DELAY_MS);
        } else if (gear == GEAR_R) {
            gearShifter.postDelayed(() ->
                    startActivity(new Intent(this, CameraActivity.class)), SHIFT_DELAY_MS);
        }
    }

    // ── Current spot card ─────────────────────────────────────────────────────

    private void refreshSessionViews() {
        boolean parked = currentSession != null;

        tvStatusPill.setText(parked ? R.string.status_parked : R.string.status_empty);
        tvStatusPill.setBackgroundResource(parked ? R.drawable.pill_parked_bg : R.drawable.pill_neutral_bg);
        tvStatusPill.setTextColor(getColor(parked ? R.color.status_parked : R.color.text_secondary));

        tvNoParkingMessage.setVisibility(parked ? View.GONE : View.VISIBLE);
        tvSavedAt.setVisibility(parked ? View.VISIBLE : View.GONE);
        tvSavedAgo.setVisibility(parked ? View.VISIBLE : View.GONE);
        actionButtons.setVisibility(parked ? View.VISIBLE : View.GONE);
        btnViewMap.setEnabled(parked);
        btnClear.setEnabled(parked);

        if (!parked) {
            tagsRow.setVisibility(View.GONE);
            tvNotes.setVisibility(View.GONE);
            return;
        }

        long createdAt = currentSession.getCreatedAt();
        tvSavedAt.setText(getString(R.string.label_saved_at,
                DateUtils.formatDateTime(this, createdAt,
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_WEEKDAY
                                | DateUtils.FORMAT_ABBREV_WEEKDAY)));
        tvSavedAgo.setText(DateUtils.getRelativeTimeSpanString(createdAt,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

        boolean hasLevel = showIfPresent(tvLevel, R.string.label_level, currentSession.getLevel());
        boolean hasSection = showIfPresent(tvSection, R.string.label_section, currentSession.getSection());
        tagsRow.setVisibility(hasLevel || hasSection ? View.VISIBLE : View.GONE);
        showIfPresent(tvNotes, R.string.label_notes, currentSession.getNote());
    }

    private boolean showIfPresent(TextView view, int formatRes, String value) {
        boolean present = value != null && !value.isEmpty();
        if (present) {
            view.setText(getString(formatRes, value));
        }
        view.setVisibility(present ? View.VISIBLE : View.GONE);
        return present;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadLatestSession() {
        parkingService.getHistory((sessions, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                Log.e(TAG, "Failed to load parking session", error);
                Toast.makeText(this, R.string.error_load_session, Toast.LENGTH_LONG).show();
                return;
            }

            currentSession = null;
            if (sessions != null) {
                for (ParkingSession s : sessions) {
                    if (s.getClosedAt() == null) {
                        currentSession = s;
                        break;
                    }
                }
            }
            refreshSessionViews();
        });
    }

    private void openMap() {
        if (currentSession != null) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("session_id", currentSession.getId());
            startActivity(intent);
        }
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_clear)
                .setMessage(R.string.confirm_clear_body)
                .setPositiveButton(R.string.yes, (d, w) -> clearCurrentSession())
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
                Log.e(TAG, "Failed to close parking session", error);
                Toast.makeText(this, R.string.error_close_session, Toast.LENGTH_LONG).show();
                return;
            }

            if (!Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.error_session_already_closed, Toast.LENGTH_LONG).show();
                loadLatestSession();
                return;
            }

            currentSession = null;
            refreshSessionViews();
        });
    }
}
