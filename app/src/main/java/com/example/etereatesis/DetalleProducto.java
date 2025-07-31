package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.etereatesis.adaptadores.CarouselAdapter;
import com.example.etereatesis.models.Perfume;
import com.squareup.picasso.Picasso;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DetalleProducto extends AppCompatActivity {

    private static final String TAG = "DETALLE_DEBUG";
    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;
    private static final String PREF_NAME = "LoginPrefs";

    // Carrusel principal
    private ViewPager2 viewPager;
    private DotsIndicator dotsIndicator;

    // Carrusel recomendados
    private ViewPager2 carouselFinal;
    private DotsIndicator dotsIndicatorFinal;
    private ImageButton btnFinalLeftArrow, btnFinalRightArrow;
    private final List<Perfume> recommendedList = new ArrayList<>();
    private CarouselAdapter carouselFinalAdapter;

    // Datos del perfume actual
    private int    currentPerfumeId;
    private int    currentCantidad;
    private double currentPrecio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_producto);

        /* ---------- Insets para notch / barras ---------- */
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        /* ---------- Referencias UI ---------- */
        viewPager           = findViewById(R.id.viewPagerImages);
        dotsIndicator       = findViewById(R.id.dotsIndicator);

        carouselFinal       = findViewById(R.id.carouselFinal);
        dotsIndicatorFinal  = findViewById(R.id.dotsIndicatorFinal);
        btnFinalLeftArrow   = findViewById(R.id.btnFinalLeftArrow);
        btnFinalRightArrow  = findViewById(R.id.btnFinalRightArrow);

        Button btnAddCart   = findViewById(R.id.btnAddCart);

        TextView txtMarca        = findViewById(R.id.txtMarcaDetalle);
        TextView txtNombre       = findViewById(R.id.txtNombreDetalle);
        TextView txtPrecio       = findViewById(R.id.txtPrecioDetalle);
        //TextView txtCantidad     = findViewById(R.id.txtCantidadDetalle);
        TextView txtDescripcion  = findViewById(R.id.txtDescripcionDetalle);
        TextView txtPresentacion = findViewById(R.id.txtTamanoDetalle);

        /* ---------- Datos recibidos ---------- */
        Intent intent       = getIntent();
        currentPerfumeId    = intent.getIntExtra("id", -1);
        String nombre       = intent.getStringExtra("nombre");
        currentPrecio       = intent.getDoubleExtra("precio", 0.0);
        String rutaImagen   = intent.getStringExtra("imagenRuta");
        currentCantidad     = intent.getIntExtra("cantidad", 1);
        String descripcion  = intent.getStringExtra("descripcion");
        int    presentML    = intent.getIntExtra("presentacionML", 0);
        String marca        = intent.getStringExtra("marca");

        Log.d(TAG, "Intent → id=" + currentPerfumeId +
                ", precio=" + currentPrecio + ", marca=" + marca);

        /* ---------- Poblamos vistas ---------- */
        txtMarca.setText(marca != null ? marca : "");
        txtNombre.setText(nombre);
        txtPrecio.setText(String.format("$%.2f", currentPrecio));
        //txtCantidad.setText("Cantidad: " + currentCantidad);
        txtDescripcion.setText(descripcion);
        txtPresentacion.setText("Presentación: " + presentML + " ml");

        /* ---------- Visibilidad botones ---------- */
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int clientId = prefs.getInt("clientId", -1);
        if (clientId < 0) {
            btnAddCart.setVisibility(View.GONE);
        } else {
            btnAddCart.setOnClickListener(v -> addToCart(currentPerfumeId, 1, clientId));
        }

        /* ---------- Carrusel principal ---------- */
        List<String> images = new ArrayList<>();
        images.add(rutaImagen);
        viewPager.setAdapter(new ImagePagerAdapter(images));
        dotsIndicator.setViewPager2(viewPager);

        /* ---------- Carrusel recomendados ---------- */
        carouselFinalAdapter = new CarouselAdapter(
                recommendedList,
                perfume -> {                          // ← callback de clic
                    Intent i = new Intent(this, DetalleProducto.class);
                    i.putExtra("id",            perfume.getId());
                    i.putExtra("nombre",        perfume.getNombre());
                    i.putExtra("precio",        perfume.getPrecio());
                    i.putExtra("imagenRuta",    perfume.getImagenRuta());
                    i.putExtra("descripcion",   perfume.getDescripcion());
                    i.putExtra("presentacionML",      perfume.getPresentacionML());
                    i.putExtra("marca",               perfume.getNombreMarca());
                    i.putExtra("cantidad",      perfume.getCantidad());
                    startActivity(i);
                }
        );
        carouselFinal.setAdapter(carouselFinalAdapter);
        dotsIndicatorFinal.setViewPager2(carouselFinal);

        carouselFinal.setOffscreenPageLimit(3);
        carouselFinal.setPageTransformer(new MarginPageTransformer(24));
        carouselFinal.setPageTransformer((page, pos) -> page.setScaleY(1 - Math.abs(pos) * 0.1f));

        btnFinalLeftArrow .setOnClickListener(v -> {
            int pos = carouselFinal.getCurrentItem();
            if (pos > 0) carouselFinal.setCurrentItem(pos - 1, true);
        });
        btnFinalRightArrow.setOnClickListener(v -> {
            int pos = carouselFinal.getCurrentItem();
            if (pos < carouselFinalAdapter.getItemCount() - 1)
                carouselFinal.setCurrentItem(pos + 1, true);
        });

        /* ---------- Cargamos recomendados ---------- */
        cargarPerfumes();
    }

    /* ==================== Carrito ==================== */
    private void addToCart(int perfumeId, int cantidad, int clientId) {
        new Thread(() -> {
            String resultado;
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement st = cn.prepareCall("{ call sp_AgregarCarrito(?, ?, ?) }")) {

                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                st.setInt(1, perfumeId);
                st.setInt(2, clientId);
                st.setInt(3, cantidad);
                st.execute();
                resultado = "Añadido al carrito ✅";
            } catch (Exception e) {
                Log.e(TAG, "addToCart", e);
                resultado = "Error al añadir: " + e.getMessage();
            }
            String finalRes = resultado;
            runOnUiThread(() -> Toast.makeText(this, finalRes, Toast.LENGTH_SHORT).show());
        }).start();
    }

    /* ============ Recomendar otros perfumes ============ */
    private void cargarPerfumes() {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement st = cn.prepareCall("{ call ObtenerPerfumes }");
                ResultSet rs = st.executeQuery();

                recommendedList.clear();
                while (rs.next()) {
                    Perfume p = new Perfume(
                            rs.getInt("id"),
                            rs.getString("nombre_perfume"),
                            rs.getDouble("precio_en_pesos"),
                            rs.getString("imagen1"),
                            rs.getString("codigo_upc"),
                            rs.getString("descripcion"),
                            rs.getInt("presentacion_ml"),
                            rs.getString("nombre_marca")
                    );
                    recommendedList.add(p);
                }
                rs.close(); st.close(); cn.close();

                runOnUiThread(() -> carouselFinalAdapter.updateData(recommendedList));

            } catch (Exception e) {
                Log.e(TAG, "cargarPerfumes", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Error al cargar perfumes", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /* ============ Adapter imágenes principales ============ */
    private static class ImagePagerAdapter
            extends RecyclerView.Adapter<ImagePagerAdapter.VH> {

        private final List<String> urls;
        ImagePagerAdapter(List<String> urls) { this.urls = urls; }

        @Override public VH onCreateViewHolder(ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_image_carousel, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            Picasso.get()
                    .load(urls.get(pos))
                    .placeholder(R.drawable.ic_placeholder)
                    .fit().centerCrop()
                    .into(h.img);
        }
        @Override public int getItemCount() { return urls.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView img; VH(View v){ super(v); img = v.findViewById(R.id.carouselImage); }
        }
    }
}
