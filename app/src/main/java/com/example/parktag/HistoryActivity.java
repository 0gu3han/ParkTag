package com.example.parktag;

import android.os.Bundle;
import android.view.View;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        toolbar.setNavigationOnClickListener(v -> finish());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        parkingService.getHistory(sessions -> {
            if (sessions == null || sessions.isEmpty()) {
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
