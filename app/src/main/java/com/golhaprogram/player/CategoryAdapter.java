package com.golhaprogram.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
    public interface Listener { void onCategory(Category category); }
    private final List<Category> items;
    private final Listener listener;

    public CategoryAdapter(List<Category> items, Listener listener) {
        this.items = items; this.listener = listener;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new Holder(v);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Category c = items.get(position);
        h.text.setText(c.title);
        h.itemView.setOnClickListener(v -> listener.onCategory(c));
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView text;
        Holder(View v) { super(v); text = v.findViewById(R.id.categoryText); }
    }
}
