package com.example.etereatesis.adaptadores;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.etereatesis.R;
import com.example.etereatesis.models.Perfume;

import java.util.ArrayList;
import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private static final String TAG = "CarouselAdapter";
    private List<Perfume> perfumes;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Perfume perfume);
    }

    public CarouselAdapter(List<Perfume> perfumes, OnItemClickListener listener) {
        this.perfumes = new ArrayList<>(perfumes);
        this.listener = listener;
        Log.d(TAG, "Adapter creado con " + this.perfumes.size() + " items");
    }

    /**
     * Actualiza la lista de perfumes y refresca el carrusel
     */
    public void updateData(List<Perfume> newList) {
        Log.d(TAG, "updateData: recibiendo " + newList.size() + " items");
        perfumes.clear();
        perfumes.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel_perfume, parent, false);
        Log.d(TAG, "onCreateViewHolder inflando item_carousel_perfume");
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Perfume perfume = perfumes.get(position);
        Log.d(TAG, "onBindViewHolder pos=" + position + " nombre=" + perfume.getNombre());

        // Carga imagen con Glide
        Glide.with(holder.itemView.getContext())
                .load(perfume.getImagenRuta())
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.imageView);

        holder.imageView.setContentDescription(perfume.getNombre());

        // Click
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Click en carousel pos=" + position + " id=" + perfume.getId());
            listener.onItemClick(perfume);
        });
    }

    @Override
    public int getItemCount() {
        int count = perfumes.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgCarousel);
        }
    }
}
