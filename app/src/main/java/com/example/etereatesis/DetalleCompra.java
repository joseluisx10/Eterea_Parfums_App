package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.adaptadores.DetalleCompraAdapter;
import com.example.etereatesis.models.CarritoItem;
import com.google.android.material.appbar.MaterialToolbar;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetalleCompra extends AppCompatActivity {

    private RecyclerView recyclerViewDetalleCompra;
    private DetalleCompraAdapter detalleAdapter;

    private TextView tvSubtotalCompra, tvDescuentoCompra, tvTotalCompra;
    private Button btnProcederPagoFinal, btnModificarDomicilio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_compra);


        Toolbar toolbar = findViewById(R.id.toolbarDetalleCompra);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Perfumes");

        // Ajustar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        tvSubtotalCompra = findViewById(R.id.tvSubtotalCompra);
        tvDescuentoCompra = findViewById(R.id.tvDescuentoCompra);
        tvTotalCompra = findViewById(R.id.tvTotalCompra);
        btnProcederPagoFinal = findViewById(R.id.btnProcederPagoFinal);
        btnModificarDomicilio = findViewById(R.id.btnModificarDomicilio);

        recyclerViewDetalleCompra = findViewById(R.id.recyclerViewDetalleCompra);
        recyclerViewDetalleCompra.setLayoutManager(new LinearLayoutManager(this));

        // Recibir la lista y los montos
        ArrayList<CarritoItem> cartItems = (ArrayList<CarritoItem>) getIntent().getSerializableExtra("LISTA_CARRITO");
        double subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0);
        double descuento = getIntent().getDoubleExtra("DESCUENTO", 0);
        double total = getIntent().getDoubleExtra("TOTAL", 0);

        detalleAdapter = new DetalleCompraAdapter(cartItems, this);
        recyclerViewDetalleCompra.setAdapter(detalleAdapter);

        Locale localeArg = new Locale("es", "AR");
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(localeArg);

        tvSubtotalCompra.setText("Subtotal: " + numberFormat.format(subtotal));
        tvDescuentoCompra.setText("Descuento Aplicado: " + numberFormat.format(descuento));
        tvTotalCompra.setText("TOTAL A PAGAR: " + numberFormat.format(total));


        // Obtener el ID del cliente (por ejemplo, desde SharedPreferences)
        SharedPreferences sp = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        int clienteId = sp.getInt("clientId", -1);
        if (clienteId != -1) {
            // Llamar a la función que obtiene y concatena la dirección.
            obtenerDireccionClienteConcatenada(clienteId);
        } else {
            Toast.makeText(this, "No se encontró el ID del cliente.", Toast.LENGTH_SHORT).show();
        }

        btnProcederPagoFinal.setOnClickListener(v -> {
            SharedPreferences sp2 = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            int clientId = sp2.getInt("clientId", 1); // Si no se encuentra, se usa 1 como ejemplo

            String formaPago = "Efectivo";

            double subtotal_ = getIntent().getDoubleExtra("SUBTOTAL", 0);
            double descuento_ = getIntent().getDoubleExtra("DESCUENTO", 0);
            double total_ = getIntent().getDoubleExtra("TOTAL", 0);

            ArrayList<CarritoItem> cartItems_ = (ArrayList<CarritoItem>) getIntent().getSerializableExtra("LISTA_CARRITO");

            insertarFactura(clientId, formaPago, total, descuento, cartItems);
        });

        btnModificarDomicilio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DetalleCompra.this, VerPerfil.class));
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void insertarFactura(final int clienteId, final String formaPago, final double precioTotal,
                                 final double descuento, final List<CarritoItem> detalleItems) {
        new Thread(() -> {
            Connection conexion = null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conexion = DriverManager.getConnection(DETECTOR.CONNECTION_URL);

                // Construir XML de detalles
                StringBuilder xml = new StringBuilder();
                xml.append("<Detalles>");
                for (CarritoItem item : detalleItems) {
                    xml.append("<Detalle ");
                    xml.append("perfume_id=\"").append(item.getId()).append("\" ");
                    xml.append("cantidad=\"").append(item.getCantidad()).append("\" ");
                    xml.append("precio_unitario=\"").append(item.getPrecioConDescuento()).append("\" ");
                    xml.append("promocion_id=\"1\" />"); // Hardcode promocion_id a 0
                }
                xml.append("</Detalles>");

                CallableStatement stmt = conexion.prepareCall("{ call sp_InsertarFactura(?, ?, ?, ?, ?) }");
                stmt.setInt(1, clienteId);
                stmt.setString(2, formaPago);
                stmt.setDouble(3, precioTotal);
                stmt.setDouble(4, descuento);
                stmt.setString(5, xml.toString());

                stmt.execute();
                stmt.close();

                runOnUiThread(() -> Toast.makeText(DetalleCompra.this,
                        "Factura guardada con éxito.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(DetalleCompra.this,
                        "Error al guardar la factura: " + e.getMessage(), Toast.LENGTH_LONG).show());
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

    private void obtenerDireccionClienteConcatenada(final int clienteId) {
        new Thread(() -> {
            Connection conexion = null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conexion = DriverManager.getConnection(DETECTOR.CONNECTION_URL);

                // Llamamos al SP que une las tablas catálogo para obtener los nombres
                CallableStatement stmt = conexion.prepareCall("{ call sp_ObtenerDireccionCliente(?) }");
                stmt.setInt(1, clienteId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // Obtenemos los datos ya con los nombres
                    String pais = rs.getString("pais");
                    String provincia = rs.getString("provincia");
                    String localidad = rs.getString("localidad");
                    String codigoPostal = rs.getString("codigo_postal");
                    String calle = rs.getString("calle");
                    String numeracionCalle = rs.getString("numeracion_calle");
                    String piso = rs.getString("piso");
                    String departamento = rs.getString("departamento");
                    String comentarios = rs.getString("comentarios_domicilio");

                    // Construir la dirección concatenada
                    StringBuilder direccion = new StringBuilder();
                    if (calle != null && !calle.trim().isEmpty()) {
                        direccion.append(calle);
                    }
                    if (numeracionCalle != null && !numeracionCalle.trim().isEmpty()) {
                        direccion.append(" ").append(numeracionCalle);
                    }
                    if (piso != null && !piso.trim().isEmpty()) {
                        direccion.append(", Piso: ").append(piso);
                    }
                    if (departamento != null && !departamento.trim().isEmpty()) {
                        direccion.append(", Dept: ").append(departamento);
                    }
                    if (codigoPostal != null && !codigoPostal.trim().isEmpty()) {
                        direccion.append(", CP: ").append(codigoPostal);
                    }
                    if (localidad != null && !localidad.trim().isEmpty()) {
                        direccion.append(", ").append(localidad);
                    }
                    if (provincia != null && !provincia.trim().isEmpty()) {
                        direccion.append(", ").append(provincia);
                    }
                    if (pais != null && !pais.trim().isEmpty()) {
                        direccion.append(", ").append(pais);
                    }
                    if (comentarios != null && !comentarios.trim().isEmpty()) {
                        direccion.append(" - ").append(comentarios);
                    }

                    runOnUiThread(() -> {
                        TextView tvDireccionEntrega = findViewById(R.id.tvDireccionEntrega);
                        tvDireccionEntrega.setText(direccion.toString());
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No se encontró dirección para el cliente.", Toast.LENGTH_SHORT).show());
                }

                rs.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al obtener dirección: " + e.getMessage(), Toast.LENGTH_LONG).show());
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

}
