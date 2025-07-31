package com.example.etereatesis.adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.R;
import com.example.etereatesis.models.DetalleItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DetalleItemAdapter extends RecyclerView.Adapter<DetalleItemAdapter.ViewHolder> {

    private List<DetalleItem> listaDetalleItems;
    private Context context;

    public DetalleItemAdapter(List<DetalleItem> listaDetalleItems, Context context) {
        this.listaDetalleItems = listaDetalleItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_detalle_factura, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetalleItem item = listaDetalleItems.get(position);

        // Mostramos la descripción en lugar del ID
        holder.tvDescripcion.setText("Descripción: " + item.getDescripcion());
        holder.tvCantidad.setText("Cantidad: " + item.getCantidad());

        // Formateamos el precio unitario y el subtotal a formato moneda (AR)
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        holder.tvPrecioUnitario.setText("Precio Unitario: " + nf.format(item.getPrecioUnitario()));
        holder.tvSubtotalItem.setText("Subtotal: " + nf.format(item.getSubtotalItem()));

        // Promoción ID (opcional)
        holder.tvPromocionId.setText("Promoción ID: " + item.getPromocionId());
    }

    @Override
    public int getItemCount() {
        return listaDetalleItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescripcion, tvCantidad, tvPrecioUnitario, tvSubtotalItem, tvPromocionId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescripcion    = itemView.findViewById(R.id.tvDescripcion);
            tvCantidad       = itemView.findViewById(R.id.tvCantidad);
            tvPrecioUnitario = itemView.findViewById(R.id.tvPrecioUnitario);
            tvSubtotalItem   = itemView.findViewById(R.id.tvSubtotalItem);
            tvPromocionId    = itemView.findViewById(R.id.tvPromocionId);
        }
    }
}
