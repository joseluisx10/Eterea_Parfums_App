package com.example.etereatesis;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.activity.EdgeToEdge;

import com.example.etereatesis.DataBaseHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AgregarPerfume extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgPerfume;
    private EditText edtNombre, edtPrecio;
    private Uri imagenUri;
    private String imagenRuta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_agregar_perfume);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar vistas
        imgPerfume = findViewById(R.id.imgPerfume);
        edtNombre = findViewById(R.id.edtNombre);
        edtPrecio = findViewById(R.id.edtPrecio);
        Button btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen);
        Button btnGuardarPerfume = findViewById(R.id.btnGuardarPerfume);

        // Configurar listeners
        btnSeleccionarImagen.setOnClickListener(v -> seleccionarImagen());
        btnGuardarPerfume.setOnClickListener(v -> guardarPerfume());
    }

    // Método para abrir la galería y seleccionar una imagen
    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Manejar el resultado de la selección de imagen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imagenUri = data.getData();
            imgPerfume.setImageURI(imagenUri);
            imagenRuta = guardarImagenLocal(imagenUri);
        }
    }

    // Guardar la imagen seleccionada localmente y retornar la ruta
    private String guardarImagenLocal(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File directorio = new File(getFilesDir(), "imagenes_perfumes");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }
            String nombreArchivo = "perfume_" + System.currentTimeMillis() + ".jpg";
            File archivo = new File(directorio, nombreArchivo);
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                return archivo.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Guardar el perfume en SQL Server
    private void guardarPerfume() {
        String nombre = edtNombre.getText().toString().trim();
        String precioStr = edtPrecio.getText().toString().trim();

        if (nombre.isEmpty() || precioStr.isEmpty() || imagenRuta == null) {
            Toast.makeText(this, "Completa todos los campos y selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Insertar en SQL Server en un hilo separado
        new Thread(() -> {
            boolean success = DataBaseHelper.insertarPerfume(nombre, precio, imagenRuta);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Perfume guardado correctamente", Toast.LENGTH_SHORT).show();
                    finish(); // Cerrar la actividad
                } else {
                    Toast.makeText(this, "Error al guardar el perfume", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
