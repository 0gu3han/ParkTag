package com.example.parktag;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.service.ParkingService;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ParkingRepository repository = new ParkingRepository(
                ParkingDatabase.getInstance(this).parkingSessionDao()
        );
        ParkingService parkingService = new ParkingService(repository);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        TextView tvEmpty = findViewById(R.id.tvEmptyHistory);

        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        parkingService.getHistory(sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
                rvHistory.setAdapter(new ParkingHistoryAdapter(sessions));
            }
        });
    }
}
