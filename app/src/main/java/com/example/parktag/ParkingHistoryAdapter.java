package com.example.parktag;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parktag.model.ParkingSession;

import java.util.Calendar;
import java.util.List;

public class ParkingHistoryAdapter extends RecyclerView.Adapter<ParkingHistoryAdapter.ViewHolder> {

    private static final int DATE_FLAGS = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_WEEKDAY
            | DateUtils.FORMAT_ABBREV_WEEKDAY;

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
        Context context = holder.itemView.getContext();

        holder.tvSavedAt.setText(DateUtils.formatDateTime(context, session.getCreatedAt(), DATE_FLAGS));

        Long closedAt = session.getClosedAt();
        if (closedAt == null) {
            holder.tvStatus.setText(R.string.status_active);
            holder.tvStatus.setBackgroundResource(R.drawable.pill_parked_bg);
            holder.tvStatus.setTextColor(context.getColor(R.color.status_parked));
        } else {
            // Include the date when the session ended on a different day than it started
            int endFlags = isSameDay(session.getCreatedAt(), closedAt)
                    ? DateUtils.FORMAT_SHOW_TIME
                    : DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
            holder.tvStatus.setText(context.getString(R.string.status_ended,
                    DateUtils.formatDateTime(context, closedAt, endFlags)));
            holder.tvStatus.setBackgroundResource(R.drawable.pill_neutral_bg);
            holder.tvStatus.setTextColor(context.getColor(R.color.text_secondary));
        }

        boolean hasLevel = showIfPresent(holder.tvLevel, R.string.label_level, session.getLevel());
        boolean hasSection = showIfPresent(holder.tvSection, R.string.label_section, session.getSection());
        holder.tagsRow.setVisibility(hasLevel || hasSection ? View.VISIBLE : View.GONE);
        showIfPresent(holder.tvNotes, R.string.label_notes, session.getNote());
    }

    private static boolean isSameDay(long a, long b) {
        Calendar first = Calendar.getInstance();
        first.setTimeInMillis(a);
        Calendar second = Calendar.getInstance();
        second.setTimeInMillis(b);
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean showIfPresent(TextView view, int formatRes, String value) {
        boolean present = value != null && !value.isEmpty();
        if (present) {
            view.setText(view.getContext().getString(formatRes, value));
        }
        view.setVisibility(present ? View.VISIBLE : View.GONE);
        return present;
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSavedAt;
        TextView tvStatus;
        View tagsRow;
        TextView tvLevel;
        TextView tvSection;
        TextView tvNotes;

        ViewHolder(View itemView) {
            super(itemView);
            tvSavedAt = itemView.findViewById(R.id.tvItemSavedAt);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tagsRow = itemView.findViewById(R.id.itemTagsRow);
            tvLevel = itemView.findViewById(R.id.tvItemLevel);
            tvSection = itemView.findViewById(R.id.tvItemSection);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
        }
    }
}
