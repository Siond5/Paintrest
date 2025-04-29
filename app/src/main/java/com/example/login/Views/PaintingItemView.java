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

/**
 * RecyclerView Adapter for displaying a list of Paintings as image items.
 * <p>
 * Uses Glide to load painting images and notifies a click listener when an item is tapped.
 * </p>
 */
public class PaintingItemView extends RecyclerView.Adapter<PaintingItemView.PaintingViewHolder> {

    /**
     * Listener interface for painting click events.
     */
    public interface OnPaintingClickListener {
        /**
         * Callback invoked when a painting item is clicked.
         *
         * @param painting The Painting object that was clicked.
         */
        void onPaintingClick(Painting painting);
    }

    /**
     * List of Painting objects to display.
     */
    private final List<Painting> paintings;

    /**
     * Listener to notify when a painting is clicked.
     */
    private final OnPaintingClickListener clickListener;

    /**
     * Constructs the adapter with the data set and click listener.
     *
     * @param paintings    List of Painting items to display.
     * @param clickListener Listener for item click events.
     */
    public PaintingItemView(List<Painting> paintings, OnPaintingClickListener clickListener) {
        this.paintings = paintings;
        this.clickListener = clickListener;
    }

    /**
     * Inflates the item view and creates a ViewHolder.
     *
     * @param parent   The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new PaintingViewHolder.
     */
    @NonNull
    @Override
    public PaintingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_painting, parent, false);
        return new PaintingViewHolder(view);
    }

    /**
     * Binds painting data to the ViewHolder's views.
     * <p>
     * Loads the image URL into the ImageView and sets up the click listener.
     * </p>
     *
     * @param holder   The ViewHolder to bind data to.
     * @param position The position of the item in the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull PaintingViewHolder holder, int position) {
        Painting painting = paintings.get(position);
        Glide.with(holder.imageView.getContext())
                .load(painting.getImageUrl())
                .into(holder.imageView);
        holder.itemView.setOnClickListener(v -> clickListener.onPaintingClick(painting));
    }

    /**
     * Returns the total number of painting items.
     *
     * @return The size of the paintings list.
     */
    @Override
    public int getItemCount() {
        return paintings.size();
    }

    /**
     * ViewHolder class for painting items, holding references to the views.
     */
    public static class PaintingViewHolder extends RecyclerView.ViewHolder {
        /**
         * ImageView displaying the painting image.
         */
        ImageView imageView;

        /**
         * Constructs a PaintingViewHolder and finds subviews.
         *
         * @param itemView The item view inflated for a painting.
         */
        public PaintingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.paintingImageView);
        }
    }
}
