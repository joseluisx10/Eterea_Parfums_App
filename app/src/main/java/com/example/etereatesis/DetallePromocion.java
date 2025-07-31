package com.example.etereatesis;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Picasso;

public class DetallePromocion extends AppCompatActivity {

    private ImageView imgDetallePromocion;
    private TextView txtNombre, txtFechaInicio, txtFechaFin, txtDescuento, txtActivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_promocion);

        // Configurar la Toolbar con la flecha de regreso
        Toolbar toolbar = findViewById(R.id.toolbarDetalle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle de Promoción");
        }

        // Inicializar vistas
        imgDetallePromocion = findViewById(R.id.imgDetallePromocion);
        txtNombre = findViewById(R.id.txtNombre);
        txtFechaInicio = findViewById(R.id.txtFechaInicio);
        txtFechaFin = findViewById(R.id.txtFechaFin);
        txtDescuento = findViewById(R.id.txtDescuento);
        txtActivo = findViewById(R.id.txtActivo);

        // Recuperar los datos enviados desde la actividad Promociones
        String nombre = getIntent().getStringExtra("nombre");
        String fechaInicio = getIntent().getStringExtra("fechaInicio");
        String fechaFin = getIntent().getStringExtra("fechaFin");
        double descuento = getIntent().getDoubleExtra("descuento", 0.0);
        boolean activo = getIntent().getBooleanExtra("activo", false);
        String url = getIntent().getStringExtra("url");

        // Cargar la imagen usando Picasso
        Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_placeholder) // Imagen de carga
                .error(R.drawable.ic_error)             // Imagen de error
                .into(imgDetallePromocion);

        // Mostrar los datos en los TextView
        txtNombre.setText(nombre);
        txtFechaInicio.setText("📅 Fecha inicio: " + fechaInicio);
        txtFechaFin.setText("📅 Fecha fin: " + fechaFin);
        txtDescuento.setText("🔥 Descuento: " + descuento + "%");
        txtActivo.setText(activo ? "✅ Activo" : "❌ No activo");
    }

    // Manejar el botón de "atrás" en la Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Cierra la actividad y regresa
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
