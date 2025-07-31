package com.example.etereatesis.adaptadores;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.etereatesis.R;
import com.example.etereatesis.models.Banner;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerVH> {
    public interface OnBannerClick { void onClick(Banner b); }

    private static final String TAG = "BannerAdapter";

    private final List<Banner> banners;
    private final OnBannerClick listener;

    public BannerAdapter(List<Banner> banners, OnBannerClick listener) {
        this.banners  = banners;
        this.listener = listener;
        Log.d(TAG, "Adapter creado con " + banners.size() + " banners");
    }

    @NonNull
    @Override
    public BannerVH onCreateViewHolder(@NonNull ViewGroup p, int vType) {
        Log.d(TAG, "onCreateViewHolder llamado");
        ImageView iv = new ImageView(p.getContext());
        iv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new BannerVH(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerVH h, int i) {
        Banner b = banners.get(i);
        Log.d(TAG, "onBindViewHolder: cargando imagen de " + b.getImagenRuta());
        Glide.with(h.img.getContext())
                .load(b.getImagenRuta())
                .placeholder(R.drawable.ic_placeholder)
                .into(h.img);
        h.img.setOnClickListener(v -> {
            Log.d(TAG, "Banner clickeado: " + b.getImagenRuta());
            listener.onClick(b);
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: " + banners.size());
        return banners.size();
    }

    public void updateData(List<Banner> nuevos){
        Log.d(TAG, "updateData: recibiendo " + nuevos.size() + " banners");
        banners.clear();
        banners.addAll(nuevos);
        notifyDataSetChanged();
    }

    static class BannerVH extends RecyclerView.ViewHolder {
        ImageView img;
        BannerVH(@NonNull ImageView iv) {
            super(iv);
            img = iv;
            Log.d(TAG, "ViewHolder creado");
        }
    }
}
