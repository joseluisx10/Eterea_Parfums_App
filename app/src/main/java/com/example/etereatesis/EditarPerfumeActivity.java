package com.example.etereatesis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.etereatesis.models.Perfume;

public class EditarPerfumeActivity extends AppCompatActivity {

    public static final String EXTRA_PERFUME_ID = "extra_perfume_id";
    public static final String EXTRA_PERFUME_NOMBRE = "extra_perfume_nombre";
    public static final String EXTRA_PERFUME_PRECIO = "extra_perfume_precio";
    public static final String EXTRA_PERFUME_IMAGEN = "extra_perfume_imagen";

    private EditText edtNombre, edtPrecio, edtImagen;
    private Button btnGuardar;

    private int perfumeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfume);

        edtNombre = findViewById(R.id.edtNombre);
        edtPrecio = findViewById(R.id.edtPrecio);
        edtImagen = findViewById(R.id.edtImagen);
        btnGuardar = findViewById(R.id.btnGuardar);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_PERFUME_ID)) {
            perfumeId = intent.getIntExtra(EXTRA_PERFUME_ID, -1);
            String nombre = intent.getStringExtra(EXTRA_PERFUME_NOMBRE);
            double precio = intent.getDoubleExtra(EXTRA_PERFUME_PRECIO, 0.0);
            String imagen = intent.getStringExtra(EXTRA_PERFUME_IMAGEN);

            edtNombre.setText(nombre);
            edtPrecio.setText(String.valueOf(precio));
            edtImagen.setText(imagen);
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nombre = edtNombre.getText().toString().trim();
        String precioStr = edtPrecio.getText().toString().trim();
        String imagen = edtImagen.getText().toString().trim();

        if (nombre.isEmpty() || precioStr.isEmpty() || imagen.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

    }
}
