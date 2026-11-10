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

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        parkingService.getHistory((sessions, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (error != null) {
                android.util.Log.e("ParkTag", "Failed to load parking history", error);

                tvEmpty.setText("Could not load parking history. Please reopen this screen to retry.");
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);

                return;
            }

            if (sessions == null || sessions.isEmpty()) {
                tvEmpty.setText("No parking history yet.");
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
