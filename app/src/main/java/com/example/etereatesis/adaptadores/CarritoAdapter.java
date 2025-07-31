package com.example.etereatesis.adaptadores;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.DataBaseHelper;
import com.example.etereatesis.R;
import com.example.etereatesis.models.CarritoItem;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    public interface CarritoAdapterListener {
        // Se pasa el nuevo subtotal, descuento y total para actualizar la UI
        void onCarritoUpdated(double newSubtotal, double newDescuento, double newTotal);
    }

    private List<CarritoItem> carrito;
    private Context context;
    private CarritoAdapterListener listener; // Callback para la Activity

    public CarritoAdapter(List<CarritoItem> carrito, Context context, CarritoAdapterListener listener) {
        this.carrito = carrito;
        this.context = context;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFotoCarrito;
        TextView txtNombreCarrito, txtPrecioCarrito, txtCantidadCarrito;
        ImageButton btnSumarCantidad, btnRestarCantidad;
        Button btnEliminarCarrito;

        public ViewHolder(View itemView) {
            super(itemView);
            imgFotoCarrito = itemView.findViewById(R.id.imgFotoCarrito);
            txtNombreCarrito = itemView.findViewById(R.id.txtNombreCarrito);
            txtPrecioCarrito = itemView.findViewById(R.id.txtPrecioCarrito);
            txtCantidadCarrito = itemView.findViewById(R.id.txtCantidadCarrito);
            btnEliminarCarrito = itemView.findViewById(R.id.btnEliminarCarrito);
            btnSumarCantidad = itemView.findViewById(R.id.btnSumarCantidad);
            btnRestarCantidad = itemView.findViewById(R.id.btnRestarCantidad);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarritoItem item = carrito.get(position);

        holder.txtNombreCarrito.setText(item.getNombre());
        double precioConDesc = item.getPrecioConDescuento();

        Locale localeArg = new Locale("es", "AR");
        NumberFormat numberFormat = NumberFormat.getNumberInstance(localeArg);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        holder.txtPrecioCarrito.setText("$" + numberFormat.format(precioConDesc));

        holder.txtCantidadCarrito.setText(String.valueOf(item.getCantidad()));

        String fotoUrl = item.getFotoUrl();
        if (fotoUrl != null && fotoUrl.startsWith("http")) {
            Picasso.get()
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.imgFotoCarrito);
        } else {
            File imgFile = new File(fotoUrl);
            if (imgFile.exists()) {
                Picasso.get()
                        .load(imgFile)
                        .placeholder(R.drawable.ic_placeholder)
                        .into(holder.imgFotoCarrito);
            } else {
                holder.imgFotoCarrito.setImageResource(R.drawable.ic_placeholder);
            }
        }

        // Botón para sumar cantidad
        holder.btnSumarCantidad.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                CarritoItem currentItem = carrito.get(pos);
                int nuevaCantidad = currentItem.getCantidad() + 1;
                currentItem.setCantidad(nuevaCantidad);
                holder.txtCantidadCarrito.setText(String.valueOf(nuevaCantidad));

                SharedPreferences sp = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                int clientId = sp.getInt("clientId", -1);
                if (clientId != -1) {
                    DataBaseHelper.actualizarCantidadCarrito(clientId, currentItem.getId(), nuevaCantidad, new DataBaseHelper.Callback() {
                        @Override
                        public void onResult(String mensaje) {
                            ((Activity) context).runOnUiThread(() -> {
                                if (mensaje.startsWith("Error")) {
                                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show();
                                } else {
                                    double[] totales = ((com.example.etereatesis.CarritoActivity) context)
                                            .calcularTotalesConDescuento(carrito);
                                    listener.onCarritoUpdated(totales[0], totales[1], totales[2]);
                                }
                            });
                        }
                    });
                }
            }
        });

        // Botón para restar cantidad (mínimo 1)
        holder.btnRestarCantidad.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                CarritoItem currentItem = carrito.get(pos);
                int cantidadActual = currentItem.getCantidad();
                if (cantidadActual > 1) {
                    int nuevaCantidad = cantidadActual - 1;
                    currentItem.setCantidad(nuevaCantidad);
                    holder.txtCantidadCarrito.setText(String.valueOf(nuevaCantidad));

                    SharedPreferences sp = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                    int clientId = sp.getInt("clientId", -1);
                    if (clientId != -1) {
                        DataBaseHelper.actualizarCantidadCarrito(clientId, currentItem.getId(), nuevaCantidad, new DataBaseHelper.Callback() {
                            @Override
                            public void onResult(String mensaje) {
                                ((Activity) context).runOnUiThread(() -> {
                                    if (mensaje.startsWith("Error")) {
                                        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show();
                                    } else {
                                        double[] totales = ((com.example.etereatesis.CarritoActivity) context)
                                                .calcularTotalesConDescuento(carrito);
                                        listener.onCarritoUpdated(totales[0], totales[1], totales[2]);
                                    }
                                });
                            }
                        });
                    }
                } else {
                    Toast.makeText(context, "La cantidad mínima es 1", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Botón "Eliminar"
        holder.btnEliminarCarrito.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                SharedPreferences sp = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                int clientId = sp.getInt("clientId", -1);
                if (clientId == -1) {
                    Toast.makeText(context, "Error: No se encontró el ID del cliente.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int productId = item.getId();
                DataBaseHelper.eliminarProductoDeCarrito(clientId, productId, mensaje -> {
                    ((Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                        Toast.makeText(holder.itemView.getContext(), mensaje, Toast.LENGTH_SHORT).show();
                        if (mensaje.contains("correctamente")) {
                            carrito.remove(pos);
                            notifyItemRemoved(pos);
                            double[] totales = ((com.example.etereatesis.CarritoActivity) context)
                                    .calcularTotalesConDescuento(carrito);
                            listener.onCarritoUpdated(totales[0], totales[1], totales[2]);
                        }
                    });
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return carrito.size();
    }
}
