package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.adaptadores.ComprasAdapter;
import com.example.etereatesis.models.Factura;
import com.google.android.material.navigation.NavigationView;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;   // si lo necesitas

public class MisCompras extends AppCompatActivity {

    /* ========= PREFERENCIAS ========= */
    private static final String PREF_NAME      = "LoginPrefs";
    private static final String KEY_LOGGED_IN  = "isLoggedIn";
    private static final String KEY_IS_GUEST   = "isGuest";
    private static final String KEY_USERNAME   = "username";
    private static final String KEY_CLIENT_ID  = "clientId";
    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    /* ========= UI ========= */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;
    private RecyclerView          recyclerCompras;
    private ComprasAdapter        comprasAdapter;

    /* ========= DATOS ========= */
    private final List<Factura> listaFacturas = new ArrayList<>();

    /* ========= ESTADO SESIÓN (cacheado en onCreate/onResume) ========= */
    private boolean isLoggedIn;
    private boolean isGuest;
    private String  usuario;

    /* ----------------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* ---------- Sesión ---------- */
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLoggedIn = sp.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = sp.getBoolean(KEY_IS_GUEST, false);
        usuario    = sp.getString(KEY_USERNAME, "");

        /* 🔐 Esta pantalla requiere usuario REAL.
           Si no hay sesión o es invitado → mandar a Login y cerrar. */
        if (!isLoggedIn || isGuest) {
            startActivity(new Intent(this, Login2.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_mis_compras);

        /* ---------- Toolbar & Drawer ---------- */
        Toolbar toolbar = findViewById(R.id.toolbarMisCompras);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView = findViewById(R.id.navigation_view);

        /* ---------- Encabezado ---------- */
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText("Bienvenido, " + usuario);

        /* ---------- Visibilidad de ítems ---------- */
        Menu navMenu = navigationView.getMenu();
        boolean userReal = isLoggedIn && !isGuest; // aquí será true (ya filtramos), pero dejamos por consistencia
        navMenu.findItem(R.id.nav_profile)   .setVisible(userReal);
        navMenu.findItem(R.id.nav_purchases) .setVisible(userReal); // pantalla actual; puedes dejar visible o false
        navMenu.findItem(R.id.nav_logout)    .setVisible(userReal);
        navMenu.findItem(R.id.nav_scan)      .setVisible(userReal); // opcional
        navMenu.findItem(R.id.nav_login)     .setVisible(!userReal); // debería ser false aquí

        // Marcar esta como la seleccionada
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        navigationView.setCheckedItem(R.id.nav_purchases);

        /* ---------- Navegación Drawer ---------- */
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // Protegido redundante (por si cambia la sesión antes de navegar)
            if (!userReal) {
                Toast.makeText(this, "Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login2.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if      (id == R.id.nav_home)       startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_logout)     { sp.edit().clear().apply(); startActivity(new Intent(this, Login2.class)); finish(); }
            else if (id == R.id.nav_profile)    startActivity(new Intent(this, VerPerfil.class));
            else if (id == R.id.nav_faq)        startActivity(new Intent(this, PreguntasFrecuentes.class));
            else if (id == R.id.nav_about)      startActivity(new Intent(this, SobreNosotros.class));
            else if (id == R.id.nav_promotions) startActivity(new Intent(this, Promociones.class));
            else if (id == R.id.nav_branches)   startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_perfumes)  { startActivity(new Intent(this, Perfumes.class)); finish(); }
            else if (id == R.id.nav_scan)       startActivity(new Intent(this, ScanUPCActivity.class));
            /* nav_purchases = pantalla actual */

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        /* ---------- RecyclerView ---------- */
        recyclerCompras = findViewById(R.id.recyclerCompras);
        recyclerCompras.setLayoutManager(new LinearLayoutManager(this));
        comprasAdapter = new ComprasAdapter(listaFacturas, this, factura -> {
            Intent i = new Intent(this, FacturaDetalleActivity.class);
            i.putExtra("numFactura", factura.getNumFactura());
            startActivity(i);
        });
        recyclerCompras.setAdapter(comprasAdapter);

        cargarFacturas(); // seguro: ya sabemos que userReal
    }

    /* ----------------------------------------------------------- */
    private void cargarFacturas() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int clientId = sp.getInt(KEY_CLIENT_ID, -1);

        if (clientId < 0) {
            Toast.makeText(this, "No se encontró el ID del cliente. Inicia sesión.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Login2.class));
            finish();
            return;
        }

        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                     CallableStatement cs = cn.prepareCall("{ call sp_ObtenerFacturasPorCliente(?) }")) {

                    cs.setInt(1, clientId);
                    ResultSet rs = cs.executeQuery();

                    listaFacturas.clear();
                    while (rs.next()) {
                        listaFacturas.add(new Factura(
                                rs.getInt("num_factura"),
                                new java.util.Date(rs.getTimestamp("fecha").getTime()),
                                rs.getDouble("precio_total"),
                                rs.getDouble("descuento"),
                                rs.getString("forma_de_pago"),
                                rs.getInt("total_items")
                        ));
                    }
                    rs.close();

                    runOnUiThread(() -> comprasAdapter.notifyDataSetChanged());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Error al cargar las compras: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /* ----------------------------------------------------------- */
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLoggedIn = sp.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = sp.getBoolean(KEY_IS_GUEST, false);
        usuario    = sp.getString(KEY_USERNAME, "");

        boolean userReal = isLoggedIn && !isGuest;

        // Si por alguna razón perdió sesión o ahora es invitado → fuera
        if (!userReal) {
            startActivity(new Intent(this, Login2.class));
            finish();
            return;
        }

        // Refresca header
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText("Bienvenido, " + usuario);

        // Refresca menú
        Menu navMenu = navigationView.getMenu();
        navMenu.findItem(R.id.nav_profile)   .setVisible(true);
        navMenu.findItem(R.id.nav_purchases) .setVisible(true);
        navMenu.findItem(R.id.nav_logout)    .setVisible(true);
        navMenu.findItem(R.id.nav_scan)      .setVisible(true);
        navMenu.findItem(R.id.nav_login)     .setVisible(false);
    }

    /* ----------------------------------------------------------- */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
