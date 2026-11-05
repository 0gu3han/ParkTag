package com.example.parktag;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;

public class MainActivity extends AppCompatActivity {

    private static final int GEAR_P = 0;
    private static final int GEAR_R = 1;
    private static final int GEAR_N = 2;
    private static final int GEAR_D = 3;

    private int currentGear = GEAR_D;
    private boolean selectorInitialized = false;

    private ParkingService parkingService;
    private ParkingSession currentSession;

    // Gear views
    private FrameLayout gearContainer;
    private View gearSelector;
    private FrameLayout[] gearViews;
    private TextView[] gearLabels;

    // Status panel views
    private TextView tvGearName;
    private TextView tvGearDesc;
    private TextView tvNoParkingMessage;
    private TextView tvSavedAt;
    private TextView tvLevel;
    private TextView tvSection;
    private TextView tvNotes;
    private LinearLayout actionButtons;
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

        gearContainer = findViewById(R.id.gearContainer);
        gearSelector = findViewById(R.id.gearSelector);

        gearViews = new FrameLayout[]{
                findViewById(R.id.gearP),
                findViewById(R.id.gearR),
                findViewById(R.id.gearN),
                findViewById(R.id.gearD)
        };
        gearLabels = new TextView[]{
                findViewById(R.id.labelP),
                findViewById(R.id.labelR),
                findViewById(R.id.labelN),
                findViewById(R.id.labelD)
        };

        tvGearName = findViewById(R.id.tvGearName);
        tvGearDesc = findViewById(R.id.tvGearDesc);
        tvNoParkingMessage = findViewById(R.id.tvNoParkingMessage);
        tvSavedAt = findViewById(R.id.tvSavedAt);
        tvLevel = findViewById(R.id.tvLevel);
        tvSection = findViewById(R.id.tvSection);
        tvNotes = findViewById(R.id.tvNotes);
        actionButtons = findViewById(R.id.actionButtons);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnClear = findViewById(R.id.btnClear);

        for (int i = 0; i < gearViews.length; i++) {
            final int gear = i;
            gearViews[i].setOnClickListener(v -> onGearSelected(gear));
        }

        btnViewMap.setOnClickListener(v -> openMap());
        btnClear.setOnClickListener(v -> confirmClear());
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        // Position selector at D after first layout pass
        gearContainer.post(() -> {
            positionSelectorAt(GEAR_D, false);
            selectorInitialized = true;
        });

        loadLatestSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Return to D after coming back from P or R
        if (currentGear == GEAR_P || currentGear == GEAR_R) {
            currentGear = GEAR_D;
            if (selectorInitialized) {
                positionSelectorAt(GEAR_D, true);
            }
            updateGearLabels(GEAR_D);
        }
        loadLatestSession();
    }

    // ── Gear logic ────────────────────────────────────────────────────────────

    private void onGearSelected(int gear) {
        if (gear == currentGear) return;

        currentGear = gear;
        if (selectorInitialized) {
            positionSelectorAt(gear, true);
        }
        updateGearLabels(gear);
        updateStatusPanel(gear);

        if (gear == GEAR_P) {
            startActivity(new Intent(this, SaveParkingActivity.class));
        } else if (gear == GEAR_R) {
            startActivity(new Intent(this, CameraActivity.class));
        }
    }

    private void positionSelectorAt(int gear, boolean animate) {
        int containerH = gearContainer.getHeight();
        if (containerH == 0) return;

        int slotH = containerH / 4;
        float targetY = gear * slotH + (slotH - gearSelector.getHeight()) / 2f;

        if (animate) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(
                    gearSelector, "translationY",
                    gearSelector.getTranslationY(), targetY);
            anim.setDuration(300);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();
        } else {
            gearSelector.setTranslationY(targetY);
        }
    }

    private void updateGearLabels(int activeGear) {
        for (int i = 0; i < gearLabels.length; i++) {
            gearLabels[i].setTextColor(getColor(
                    i == activeGear ? R.color.accent_amber : R.color.gear_inactive));
        }

        String[] names = {
                getString(R.string.gear_name_park),
                getString(R.string.gear_name_reverse),
                getString(R.string.gear_name_neutral),
                getString(R.string.gear_name_drive)
        };
        tvGearName.setText(names[activeGear]);
    }

    // ── Status panel ──────────────────────────────────────────────────────────

    private void updateStatusPanel(int gear) {
        boolean isDrive = gear == GEAR_D;

        actionButtons.setVisibility(isDrive ? View.VISIBLE : View.GONE);
        tvNoParkingMessage.setVisibility(isDrive ? View.VISIBLE : View.GONE);
        tvSavedAt.setVisibility(View.GONE);
        tvLevel.setVisibility(View.GONE);
        tvSection.setVisibility(View.GONE);
        tvNotes.setVisibility(View.GONE);

        String[] descs = {
                getString(R.string.gear_desc_park),
                getString(R.string.gear_desc_reverse),
                getString(R.string.gear_desc_neutral),
                currentSession != null
                        ? getString(R.string.gear_desc_drive_saved)
                        : getString(R.string.gear_desc_drive_empty)
        };
        tvGearDesc.setText(descs[gear]);

        if (isDrive) {
            refreshSessionViews();
        }
    }

    private void refreshSessionViews() {
        if (currentSession == null) {
            tvNoParkingMessage.setVisibility(View.VISIBLE);
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
            if (note != null && !note.isEmpty()) {
                tvNotes.setText(getString(R.string.label_notes, note));
                tvNotes.setVisibility(View.VISIBLE);
            }

            btnViewMap.setEnabled(true);
            btnClear.setEnabled(true);
        }
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadLatestSession() {
        parkingService.getHistory(sessions -> {
            currentSession = null;
            if (sessions != null) {
                for (ParkingSession s : sessions) {
                    if (s.getClosedAt() == null) {
                        currentSession = s;
                        break;
                    }
                }
            }
            if (currentGear == GEAR_D) {
                updateStatusPanel(GEAR_D);
            }
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
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_clear)
                .setPositiveButton(R.string.yes, (d, w) -> clearCurrentSession())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void clearCurrentSession() {
        if (currentSession != null) {
            parkingService.closeSession(currentSession.getId(), success -> {
                currentSession = null;
                updateStatusPanel(GEAR_D);
            });
        }
    }
}
