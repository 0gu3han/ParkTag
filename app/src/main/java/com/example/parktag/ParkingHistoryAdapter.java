package com.example.parktag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parktag.model.ParkingSession;

import java.util.Date;
import java.util.List;

public class ParkingHistoryAdapter extends RecyclerView.Adapter<ParkingHistoryAdapter.ViewHolder> {

    private final List<ParkingSession> sessions;

    public ParkingHistoryAdapter(List<ParkingSession> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingSession session = sessions.get(position);

        holder.tvSavedAt.setText(new Date(session.getCreatedAt()).toString());

        if (session.getLevel() != null && !session.getLevel().isEmpty()) {
            holder.tvLevel.setText(holder.itemView.getContext()
                    .getString(R.string.label_level, session.getLevel()));
            holder.tvLevel.setVisibility(View.VISIBLE);
        } else {
            holder.tvLevel.setVisibility(View.GONE);
        }

        if (session.getSection() != null && !session.getSection().isEmpty()) {
            holder.tvSection.setText(holder.itemView.getContext()
                    .getString(R.string.label_section, session.getSection()));
            holder.tvSection.setVisibility(View.VISIBLE);
        } else {
            holder.tvSection.setVisibility(View.GONE);
        }

        if (session.getNote() != null && !session.getNote().isEmpty()) {
            holder.tvNotes.setText(holder.itemView.getContext()
                    .getString(R.string.label_notes, session.getNote()));
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSavedAt;
        TextView tvLevel;
        TextView tvSection;
        TextView tvNotes;

        ViewHolder(View itemView) {
            super(itemView);
            tvSavedAt = itemView.findViewById(R.id.tvItemSavedAt);
            tvLevel = itemView.findViewById(R.id.tvItemLevel);
            tvSection = itemView.findViewById(R.id.tvItemSection);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
        }
    }
}
