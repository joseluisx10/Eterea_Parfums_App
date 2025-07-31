package com.example.etereatesis.adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.R;
import com.example.etereatesis.models.Factura;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.ViewHolder> {

    public interface OnFacturaClickListener {
        void onFacturaClick(Factura factura);
    }

    private List<Factura> listaFacturas;
    private Context context;
    private OnFacturaClickListener listener;

    public ComprasAdapter(List<Factura> listaFacturas, Context context, OnFacturaClickListener listener) {
        this.listaFacturas = listaFacturas;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mis_compras, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Factura factura = listaFacturas.get(position);
        holder.tvNumFactura.setText("Factura Nº: " + factura.getNumFactura());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvFecha.setText("Fecha: " + sdf.format(factura.getFecha()));

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        holder.tvPrecioTotal.setText("Total: " + nf.format(factura.getPrecioTotal()));

        holder.tvFormaPago.setText("Pago: " + factura.getFormaPago());
        holder.tvTotalItems.setText("Items: " + factura.getTotalItems());

        // Agregamos el listener para detectar clicks en el item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFacturaClick(factura);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaFacturas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumFactura, tvFecha, tvPrecioTotal, tvFormaPago, tvTotalItems;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumFactura = itemView.findViewById(R.id.tvNumFactura);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvPrecioTotal = itemView.findViewById(R.id.tvPrecioTotal);
            tvFormaPago = itemView.findViewById(R.id.tvFormaPago);
            tvTotalItems = itemView.findViewById(R.id.tvTotalItems);
        }
    }
}
