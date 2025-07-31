package com.example.etereatesis.adaptadores;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.DetalleProducto;
import com.example.etereatesis.R;
import com.example.etereatesis.models.Perfume;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PerfumeAdapter extends RecyclerView.Adapter<PerfumeAdapter.ViewHolder>
        implements Filterable {

    /* ==== Callbacks ==== */
    public interface OnAddToCartListener    { void onAddToCart(Perfume p); }
    public interface OnRemoveFromCartListener { void onRemoveFromCart(Perfume p); }

    private List<Perfume> perfumes;
    private List<Perfume> perfumesFull;
    private final boolean canAddToCart;
    private final OnAddToCartListener addListener;
    private final OnRemoveFromCartListener removeListener;

    public PerfumeAdapter(List<Perfume> perfumes,
                          boolean canAddToCart,
                          OnAddToCartListener addListener,
                          OnRemoveFromCartListener removeListener) {
        this.perfumes       = perfumes;
        this.perfumesFull   = new ArrayList<>(perfumes);
        this.canAddToCart   = canAddToCart;
        this.addListener    = addListener;
        this.removeListener = removeListener;
    }

    /* ==== Recarga completa ==== */
    public void updateData(List<Perfume> newList) {
        this.perfumes     = newList;
        this.perfumesFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_perfume, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Perfume p = perfumes.get(position);

        // --- Forzar cantidad a cero si no se puede agregar (modo invitado) ---
        if (!canAddToCart && p.getCantidad() != 0) {
            p.setCantidad(0); // Asume que tu modelo tiene un setCantidad(int)
        }

        // 1. Nombre y precio
        holder.txtNombre.setText(p.getNombre());
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        holder.txtPrecio.setText(fmt.format(p.getPrecio()));

        // 2. Imagen
        String ruta = p.getImagenRuta();
        if (ruta != null && ruta.startsWith("http")) {
            Picasso.get()
                    .load(ruta)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.imgFoto);
        } else {
            File f = new File(ruta);
            Picasso.get()
                    .load(f.exists() ? f : null)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.imgFoto);
        }

        // 3. Controles de cantidad / botón agregar
        int qty = p.getCantidad();

        if (qty > 0 && canAddToCart) {
            holder.llCantidadControl.setVisibility(View.VISIBLE);
            holder.tvCantidad.setText(String.valueOf(qty));
            holder.btnAgregar.setVisibility(View.GONE);
        } else {
            holder.llCantidadControl.setVisibility(View.GONE);
            holder.btnAgregar.setVisibility(canAddToCart ? View.VISIBLE : View.GONE);
        }

        // 4. Deshabilita controles si no puede agregar (invitado)
        boolean controlsEnabled = canAddToCart;
        holder.btnSumar.setEnabled(controlsEnabled);
        holder.btnRestar.setEnabled(controlsEnabled);
        holder.btnAgregar.setEnabled(controlsEnabled);

        // 5. Listeners (con protección para invitados)
        holder.btnAgregar.setOnClickListener(v -> {
            if (!canAddToCart) return;
            addListener.onAddToCart(p);
            holder.llCantidadControl.setVisibility(View.VISIBLE);
            holder.btnAgregar.setVisibility(View.GONE);
            holder.tvCantidad.setText(String.valueOf(p.getCantidad()));
        });

        holder.btnSumar.setOnClickListener(v -> {
            if (!canAddToCart) return;
            addListener.onAddToCart(p);
            p.setCantidad(p.getCantidad() + 1);
            holder.tvCantidad.setText(String.valueOf(p.getCantidad()));
        });

        holder.btnRestar.setOnClickListener(v -> {
            if (!canAddToCart) return;
            removeListener.onRemoveFromCart(p);
            p.setCantidad(Math.max(0, p.getCantidad() - 1));
            holder.tvCantidad.setText(String.valueOf(p.getCantidad()));
            if (p.getCantidad() == 0) {
                holder.llCantidadControl.setVisibility(View.GONE);
                if (canAddToCart) holder.btnAgregar.setVisibility(View.VISIBLE);
            }
        });

        // 6. Ir a detalle (siempre disponible)
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), DetalleProducto.class);
            i.putExtra("id",            p.getId());
            i.putExtra("nombre",        p.getNombre());
            i.putExtra("precio",        p.getPrecio());
            i.putExtra("imagenRuta",    p.getImagenRuta());
            i.putExtra("cantidad",      p.getCantidad());
            i.putExtra("descripcion",   p.getDescripcion());
            i.putExtra("presentacionML",p.getPresentacionML());
            i.putExtra("marca",         p.getNombreMarca());
            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() { return perfumes.size(); }

    /* ==== Filtro por texto ==== */
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Perfume> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(perfumesFull);
                } else {
                    String pat = constraint.toString().toLowerCase().trim();
                    for (Perfume x : perfumesFull) {
                        if (x.getNombre().toLowerCase().contains(pat)) {
                            filtered.add(x);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                perfumes.clear();
                perfumes.addAll((List<Perfume>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    /* ==== ViewHolder ==== */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    imgFoto;
        TextView     txtNombre, txtPrecio, tvCantidad;
        Button       btnAgregar;
        LinearLayout llCantidadControl;
        ImageButton  btnSumar, btnRestar;

        public ViewHolder(View itemView) {
            super(itemView);
            imgFoto           = itemView.findViewById(R.id.imgFoto);
            txtNombre         = itemView.findViewById(R.id.txtNombre);
            txtPrecio         = itemView.findViewById(R.id.txtPrecio);
            btnAgregar        = itemView.findViewById(R.id.btnAgregar);
            llCantidadControl = itemView.findViewById(R.id.llCantidadControl);
            btnSumar          = itemView.findViewById(R.id.btnSumar);
            btnRestar         = itemView.findViewById(R.id.btnRestar);
            tvCantidad        = itemView.findViewById(R.id.tvCantidad);
        }
    }
}
