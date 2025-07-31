package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    /* ======== Preferencias ======== */
    private static final String PREF_NAME     = "LoginPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST  = "isGuest";
    private static final String KEY_USERNAME  = "username";

    /* ======== Drawer & menú ======== */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;

    /* ======== Sesión ======== */
    private SharedPreferences prefs;
    private boolean isLoggedIn;
    private boolean isGuest;
    private String  usuario;

    /* ======== Mapa ======== */
    private GoogleMap mMap;
    private final String direccion = "Av. Cabildo 3000,CABA, Argentina.";

    /* ======================================================= */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* ---------- Sesión ---------- */
        prefs      = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST, false);
        usuario    = prefs.getString(KEY_USERNAME, "");

        setContentView(R.layout.activity_maps);

        /* ---------- Toolbar ---------- */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* ---------- Drawer ---------- */
        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle   = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        /* Header dinámico */
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText(isLoggedIn && !isGuest
                ? "Bienvenido, " + usuario
                : "Bienvenido, invitado");

        /* Visibilidad de ítems
           - Protegidos solo para usuario REAL
           - Login visible si invitado o sin sesión */
        Menu menu = navigationView.getMenu();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        boolean userReal = isLoggedIn && !isGuest;
        menu.findItem(R.id.nav_profile)   .setVisible(userReal);
        menu.findItem(R.id.nav_purchases) .setVisible(userReal);
        menu.findItem(R.id.nav_logout)    .setVisible(userReal);
        menu.findItem(R.id.nav_scan)      .setVisible(userReal); // oculta scan a invitado
        menu.findItem(R.id.nav_login)     .setVisible(!isLoggedIn || isGuest);

        /* Marcar “Sucursales” como seleccionado */
        navigationView.setCheckedItem(R.id.nav_branches);

        /* Navegación drawer */
        navigationView.setNavigationItemSelectedListener(item -> {
            item.setChecked(true);
            int id = item.getItemId();

            /* Bloqueo selectivo:
               Si NO hay usuario real (invitado o sin sesión) y toca algo protegido → Login */
            if (!userReal) {
                if (id == R.id.nav_profile ||
                        id == R.id.nav_purchases ||
                        id == R.id.nav_logout ||
                        id == R.id.nav_scan) {
                    Toast.makeText(this, "Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, Login2.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            }

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();

            } else if (id == R.id.nav_logout) {
                // Solo debería verse si userReal, pero por si acaso...
                prefs.edit().clear().apply();
                startActivity(new Intent(this, Login2.class));
                finish();

            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, Login2.class));

            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, VerPerfil.class));

            } else if (id == R.id.nav_purchases) {
                startActivity(new Intent(this, MisCompras.class));

            } else if (id == R.id.nav_faq) {
                startActivity(new Intent(this, PreguntasFrecuentes.class));

            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, SobreNosotros.class));

            } else if (id == R.id.nav_promotions) {
                startActivity(new Intent(this, Promociones.class));

            } else if (id == R.id.nav_perfumes) {
                startActivity(new Intent(this, Perfumes.class));
                finish();

            } else if (id == R.id.nav_scan) {
                startActivity(new Intent(this, ScanUPCActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        /* ---------- Fragmento del mapa ---------- */
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /* ======================================================= */
    /*                     MAPA                                */
    /* ======================================================= */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        actualizarMapa(direccion);
    }

    private void actualizarMapa(String dir) {
        LatLng loc = obtenerCoordenadas(dir);
        if (loc != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title("Ubicación: " + dir));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        } else {
            Toast.makeText(this,
                    "Dirección no encontrada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private LatLng obtenerCoordenadas(String dir) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> list = geocoder.getFromLocationName(dir, 1);
            if (list != null && !list.isEmpty()) {
                Address addr = list.get(0);
                return new LatLng(addr.getLatitude(), addr.getLongitude());
            }
        } catch (IOException e) {
            Log.e("MapsError", "Geocoding falló", e);
        }
        return null;
    }

    /* ======================================================= */
    /*                REFRESCAR VISIBILIDAD                    */
    /* ======================================================= */
    @Override
    protected void onResume() {
        super.onResume();
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST, false);
        usuario    = prefs.getString(KEY_USERNAME, "");

        boolean userReal = isLoggedIn && !isGuest;

        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile)   .setVisible(userReal);
        menu.findItem(R.id.nav_purchases) .setVisible(userReal);
        menu.findItem(R.id.nav_logout)    .setVisible(userReal);
        menu.findItem(R.id.nav_scan)      .setVisible(userReal);
        menu.findItem(R.id.nav_login)     .setVisible(!isLoggedIn || isGuest);

        // Actualiza header
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText(userReal
                ? "Bienvenido, " + usuario
                : "Bienvenido, invitado");
    }

    /* ======================================================= */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
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
