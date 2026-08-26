package com.golhaprogram.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProgramAdapter extends RecyclerView.Adapter<ProgramAdapter.Holder> {
    public interface Listener { void onPlay(Program program, int position); }

    private final List<Program> items;
    private final Listener listener;

    public ProgramAdapter(List<Program> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_program, parent, false);
        return new Holder(v);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Program p = items.get(position);
        h.text.setText(p.displayName());
        View.OnClickListener click = v -> listener.onPlay(p, position);
        h.itemView.setOnClickListener(click);
        h.play.setOnClickListener(click);
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView text; Button play;
        Holder(View v) {
            super(v);
            text = v.findViewById(R.id.programText);
            play = v.findViewById(R.id.playItem);
        }
    }
}
