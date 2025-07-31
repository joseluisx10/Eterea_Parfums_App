package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.adaptadores.CarritoAdapter;
import com.example.etereatesis.adaptadores.CarritoAdapter.CarritoAdapterListener;
import com.example.etereatesis.models.CarritoItem;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CarritoActivity extends AppCompatActivity implements CarritoAdapterListener {

    private RecyclerView recyclerCarrito;
    private List<CarritoItem> listaCarrito = new ArrayList<>();
    private CarritoAdapter carritoAdapter;

    private TextView tvSubtotal, tvDescuento, tvTotal;
    private Button btnFinalizarPedido;

    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_CLIENT_ID = "clientId";

    private NumberFormat numberFormatArg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrito);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarCarrito);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Flecha de regreso
            getSupportActionBar().setTitle("Carrito");
        }

        Locale localeArg = new Locale("es", "AR");
        numberFormatArg = NumberFormat.getNumberInstance(localeArg);
        numberFormatArg.setMinimumFractionDigits(2);
        numberFormatArg.setMaximumFractionDigits(2);

        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDescuento = findViewById(R.id.tv_descuento);
        tvTotal = findViewById(R.id.tv_total);
        btnFinalizarPedido = findViewById(R.id.btn_finalizar_pedido);

        recyclerCarrito = findViewById(R.id.recyclerCarrito);
        recyclerCarrito.setLayoutManager(new LinearLayoutManager(this));
        carritoAdapter = new CarritoAdapter(listaCarrito, this, this);
        recyclerCarrito.setAdapter(carritoAdapter);

        btnFinalizarPedido.setOnClickListener(view -> {
            double[] totales = calcularTotalesConDescuento(listaCarrito);
            double subtotal = totales[0];
            double descuento = totales[1];
            double total = totales[2];

            // 2) Crea un ArrayList a partir de la lista (CarritoItem debe implementar Serializable)
            ArrayList<CarritoItem> cartItems = new ArrayList<>(listaCarrito);

            // 3) Crear Intent para ir a DetalleCompra
            Intent intent = new Intent(CarritoActivity.this, DetalleCompra.class);

            // 4) Pasar valores como extras
            intent.putExtra("LISTA_CARRITO", cartItems);  // lista serializable
            intent.putExtra("SUBTOTAL", subtotal);
            intent.putExtra("DESCUENTO", descuento);
            intent.putExtra("TOTAL", total);

            startActivity(intent);
        });

        cargarCarrito();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cargarCarrito() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int clientId = sharedPreferences.getInt(KEY_CLIENT_ID, -1);
        if (clientId == -1) {
            Toast.makeText(this, "No se encontró el ID del cliente.", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            Connection conexion = null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conexion = DriverManager.getConnection(CONNECTION_URL);

                CallableStatement stmt = conexion.prepareCall("{ call ObtenerCarritoPorCliente(?) }");
                stmt.setInt(1, clientId);
                ResultSet rs = stmt.executeQuery();

                listaCarrito.clear();

                while (rs.next()) {
                    int perfumeId = rs.getInt("perfumeId");
                    String nombre = rs.getString("nombre");
                    double precio = rs.getDouble("precio");
                    String fotoUrl = rs.getString("imagen1");
                    int cantidad = rs.getInt("cantidad");

                    CarritoItem item = new CarritoItem(perfumeId, nombre, precio, fotoUrl, cantidad);

                    double descuentoBD = obtenerDescuentoParaPerfumeSinAsync(perfumeId, conexion);
                    item.setDescuento(descuentoBD);

                    listaCarrito.add(item);
                }

                rs.close();
                stmt.close();

                // Calcular totales: subtotal (sin descuento), descuento acumulado y total final
                double[] totales = calcularTotalesConDescuento(listaCarrito);

                runOnUiThread(() -> {
                    carritoAdapter.notifyDataSetChanged();
                    tvSubtotal.setText("$" + numberFormatArg.format(totales[0]));
                    tvDescuento.setText("$" + numberFormatArg.format(totales[1]));
                    tvTotal.setText("$" + numberFormatArg.format(totales[2]));
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(CarritoActivity.this,
                        "Error al cargar el carrito: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            } finally {
                if (conexion != null) {
                    try {
                        conexion.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private double obtenerDescuentoParaPerfumeSinAsync(int perfumeId, Connection conexion) {
        double discount = 0.0;
        CallableStatement stmt = null;
        try {
            stmt = conexion.prepareCall("{ call sp_ObtenerDescuentoParaPerfume(?, ?) }");
            stmt.setInt(1, perfumeId);
            stmt.registerOutParameter(2, java.sql.Types.DECIMAL);
            stmt.execute();

            BigDecimal bd = stmt.getBigDecimal(2);
            if (bd != null) {
                discount = bd.doubleValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return discount;
    }

    public double[] calcularTotalesConDescuento(List<CarritoItem> items) {
        double subtotal = 0.0;
        double descuentoAcumulado = 0.0;

        for (CarritoItem cItem : items) {
            double precioOriginal = cItem.getPrecio();
            double precioConDesc = cItem.getPrecioConDescuento();
            int qty = cItem.getCantidad();

            subtotal += (precioOriginal * qty);
            descuentoAcumulado += (precioOriginal - precioConDesc) * qty;
        }

        double total = subtotal - descuentoAcumulado;
        return new double[]{subtotal, descuentoAcumulado, total};
    }

    // Este método no se usa al pasar a la actividad DetalleCompra, pero se deja para finalizarPedido() si es necesario
    private void finalizarPedido() {
        // Lógica para finalizar el pedido (SP, etc.)
    }

    @Override
    public void onCarritoUpdated(double newSubtotal, double newDescuento, double newTotal) {
        tvSubtotal.setText("$" + numberFormatArg.format(newSubtotal));
        tvDescuento.setText("$" + numberFormatArg.format(newDescuento));
        tvTotal.setText("$" + numberFormatArg.format(newTotal));
    }


}
