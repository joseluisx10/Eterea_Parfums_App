package com.example.etereatesis.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.R;
import com.example.etereatesis.models.Promocion;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdaptadorCarouselPromociones extends RecyclerView.Adapter<AdaptadorCarouselPromociones.CarouselViewHolder> {

    private final List<Promocion> promociones;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Promocion promocion);
    }

    public AdaptadorCarouselPromociones(List<Promocion> promociones, OnItemClickListener listener) {
        this.promociones = promociones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel_promocion, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Promocion promo = promociones.get(position);

        // Texto
        holder.txtNombre.setText(promo.getNombre());
        holder.txtFechas.setText(promo.getFechaInicio() + " al " + promo.getFechaFin());
        holder.txtDescuento.setText("Descuento: " + promo.getDescuento() + "%");

        // Imagen opcional
        if (promo.getUrl() != null && !promo.getUrl().isEmpty()) {
            holder.imgCarousel.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(promo.getUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(holder.imgCarousel);
        } else {
            holder.imgCarousel.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> listener.onItemClick(promo));
    }

    @Override
    public int getItemCount() {
        return promociones.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCarousel;
        TextView txtNombre, txtFechas, txtDescuento;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCarousel  = itemView.findViewById(R.id.imgCarousel);
            txtNombre    = itemView.findViewById(R.id.txtNombre);
            txtFechas    = itemView.findViewById(R.id.txtFechas);
            txtDescuento = itemView.findViewById(R.id.txtDescuento);
        }
    }
}
