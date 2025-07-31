package com.example.etereatesis.adaptadores;

import android.content.Intent;
import android.graphics.Paint;
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

import androidx.annotation.NonNull;
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

public class PromotionsAdapter
        extends RecyclerView.Adapter<PromotionsAdapter.VH>
        implements Filterable {

    public interface OnAdd { void add(Perfume p); }
    public interface OnRemove { void sub(Perfume p); }

    private List<Perfume> data;         // lista actual mostrada
    private List<Perfume> fullData;     // copia completa para filtrar
    private final boolean canAdd;
    private final OnAdd addCB;
    private final OnRemove subCB;
    private final NumberFormat fmt =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    public PromotionsAdapter(List<Perfume> data,
                             boolean canAdd,
                             OnAdd addCB,
                             OnRemove subCB) {
        this.data = data;
        this.fullData = new ArrayList<>(data);
        this.canAdd = canAdd;
        this.addCB = addCB;
        this.subCB = subCB;
    }

    public void updateData(List<Perfume> list) {
        this.data = list;
        this.fullData = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promo, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Perfume p = data.get(pos);

        // FORZAR cantidad a 0 si canAdd == false (invitado)
        if (!canAdd && p.getCantidad() != 0) {
            p.setCantidad(0);
        }

        // Imagen
        String ruta = p.getImagenRuta();
        if (ruta != null && ruta.startsWith("http")) {
            Picasso.get()
                    .load(ruta)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(h.img);
        } else {
            File f = new File(ruta == null ? "" : ruta);
            Picasso.get()
                    .load(f.exists() ? f : null)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(h.img);
        }

        // Textos
        h.txtNombre.setText(p.getNombre());
        h.txtPromo.setText(fmt.format(p.getPrecioPromocion()));
        h.txtOrig.setText(fmt.format(p.getPrecioOriginal()));
        h.txtOrig.setPaintFlags(h.txtOrig.getPaintFlags() |
                Paint.STRIKE_THRU_TEXT_FLAG);

        // Cantidad / botones
        int q = p.getCantidad();

        // --- Control de UI según canAdd ---
        if (canAdd && q > 0) {
            h.llCnt.setVisibility(View.VISIBLE);
            h.btnAdd.setVisibility(View.GONE);
            h.tvCnt.setText(String.valueOf(q));
        } else {
            h.llCnt.setVisibility(View.GONE);
            h.btnAdd.setVisibility(canAdd ? View.VISIBLE : View.GONE);
        }
        h.btnAdd.setEnabled(canAdd);
        h.btnMas.setEnabled(canAdd);
        h.btnMenos.setEnabled(canAdd);

        // Listeners protegidos por canAdd
        h.btnAdd.setOnClickListener(v -> {
            if (!canAdd) return;
            addCB.add(p);
            p.setCantidad(1);
            notifyItemChanged(pos);
        });
        h.btnMas.setOnClickListener(v -> {
            if (!canAdd) return;
            addCB.add(p);
            p.setCantidad(p.getCantidad() + 1);
            h.tvCnt.setText(String.valueOf(p.getCantidad()));
        });
        h.btnMenos.setOnClickListener(v -> {
            if (!canAdd) return;
            subCB.sub(p);
            int newQ = Math.max(0, p.getCantidad() - 1);
            p.setCantidad(newQ);
            if (newQ == 0) notifyItemChanged(pos);
            else h.tvCnt.setText(String.valueOf(newQ));
        });

        // Click → Detalle (siempre disponible)
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), DetalleProducto.class);
            i.putExtra("id", p.getId());
            i.putExtra("nombre", p.getNombre());
            i.putExtra("precio", p.getPrecioPromocion());
            i.putExtra("imagenRuta", p.getImagenRuta());
            i.putExtra("cantidad", p.getCantidad());
            i.putExtra("descripcion", p.getDescripcion());
            i.putExtra("presentacionML", p.getPresentacionML());
            i.putExtra("marca", p.getNombreMarca());
            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Perfume> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(fullData);
                } else {
                    String pat = constraint.toString().toLowerCase().trim();
                    for (Perfume x : fullData) {
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
                data.clear();
                data.addAll((List<Perfume>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    /** ViewHolder */
    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtNombre, txtPromo, txtOrig, tvCnt;
        Button btnAdd;
        ImageButton btnMas, btnMenos;
        LinearLayout llCnt;

        VH(View v) {
            super(v);
            img       = v.findViewById(R.id.imgPromo);
            txtNombre = v.findViewById(R.id.txtNombrePromo);
            txtPromo  = v.findViewById(R.id.txtPrecioPromocion);
            txtOrig   = v.findViewById(R.id.txtPrecioOriginal);
            btnAdd    = v.findViewById(R.id.btnAgregar);
            btnMas    = v.findViewById(R.id.btnSumar);
            btnMenos  = v.findViewById(R.id.btnRestar);
            tvCnt     = v.findViewById(R.id.tvCantidad);
            llCnt     = v.findViewById(R.id.llCantidadControl);
        }
    }
}
