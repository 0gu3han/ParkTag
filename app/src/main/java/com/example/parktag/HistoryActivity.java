package com.example.parktag;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.service.ParkingService;
import com.google.android.material.appbar.MaterialToolbar;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        ParkingService parkingService = new ParkingService(repository);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        View emptyState = findViewById(R.id.emptyHistory);
        TextView tvEmptyTitle = findViewById(R.id.tvEmptyHistory);
        TextView tvEmptyHint = findViewById(R.id.tvEmptyHistoryHint);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        toolbar.setNavigationOnClickListener(v -> finish());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        parkingService.getHistory((sessions, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                Log.e("ParkTag", "Failed to load parking history", error);
                tvEmptyTitle.setText(R.string.error_load_history);
                tvEmptyHint.setText(R.string.error_load_history_hint);
                emptyState.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
                return;
            }

            if (sessions == null || sessions.isEmpty()) {
                tvEmptyTitle.setText(R.string.label_empty_history);
                tvEmptyHint.setText(R.string.label_empty_history_hint);
                emptyState.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
                rvHistory.setAdapter(new ParkingHistoryAdapter(sessions));
            }
        });
    }
}
