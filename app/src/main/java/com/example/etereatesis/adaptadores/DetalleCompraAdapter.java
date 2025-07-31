package com.example.etereatesis.adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.R;
import com.example.etereatesis.models.CarritoItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DetalleCompraAdapter extends RecyclerView.Adapter<DetalleCompraAdapter.ViewHolder> {

    private List<CarritoItem> listaItems;
    private Context context;

    public DetalleCompraAdapter(List<CarritoItem> listaItems, Context context) {
        this.listaItems = listaItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_detalle_compra, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarritoItem item = listaItems.get(position);
        holder.tvNombreProducto.setText(item.getNombre());
        holder.tvCantidadProducto.setText("Cantidad: " + item.getCantidad());

        double precioConDesc = item.getPrecioConDescuento();
        Locale localeArg = new Locale("es", "AR");
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(localeArg);
        holder.tvPrecioProducto.setText("Precio: " + numberFormat.format(precioConDesc));
    }

    @Override
    public int getItemCount() {
        return listaItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreProducto, tvCantidadProducto, tvPrecioProducto;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tvNombreProducto);
            tvCantidadProducto = itemView.findViewById(R.id.tvCantidadProducto);
            tvPrecioProducto = itemView.findViewById(R.id.tvPrecioProducto);
        }
    }
}
