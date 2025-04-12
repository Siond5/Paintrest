package com.example.login.Views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.login.Classes.Painting;
import com.example.login.R;

import java.util.List;

public class PaintingItemView extends RecyclerView.Adapter<PaintingItemView.PaintingViewHolder> {

    public interface OnPaintingClickListener {
        void onPaintingClick(Painting painting);
    }

    private final List<Painting> paintings;
    private final OnPaintingClickListener clickListener;

    public PaintingItemView(List<Painting> paintings, OnPaintingClickListener clickListener) {
        this.paintings = paintings;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PaintingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_painting, parent, false);
        return new PaintingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaintingViewHolder holder, int position) {
        Painting painting = paintings.get(position);
        Glide.with(holder.imageView.getContext())
                .load(painting.getImageUrl())
                .into(holder.imageView);
        holder.itemView.setOnClickListener(v -> clickListener.onPaintingClick(painting));
    }

    @Override
    public int getItemCount() {
        return paintings.size();
    }

    public static class PaintingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PaintingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.paintingImageView);
        }
    }


}
