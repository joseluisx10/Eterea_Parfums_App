package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

public class SobreNosotros extends AppCompatActivity {

    /* ===== Prefs ===== */
    private static final String PREF_NAME     = "LoginPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST  = "isGuest";
    private static final String KEY_USERNAME  = "username";

    /* ===== UI ===== */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;

    /* ===== Sesión ===== */
    private SharedPreferences prefs;
    private boolean isLoggedIn, isGuest;

    /* --------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sobre_nosotros);

        /* ---------- Sesión ---------- */
        prefs      = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST , false);
        String usuario = prefs.getString(KEY_USERNAME, "");

        /* ---------- Toolbar & Drawer ---------- */
        Toolbar toolbar = findViewById(R.id.toolbarSobreNosotros);
        setSupportActionBar(toolbar);

        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle   = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        configurarDrawer(usuario);      // inicial

        /* Edge-to-edge padding */
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v,insets)->{
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });
    }

    /* ------------ Drawer helper ------------ */
    private void configurarDrawer(String usuario) {
        boolean userReal = isLoggedIn && !isGuest;

        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText(userReal
                ? String.format(Locale.getDefault(), "Bienvenido, %s", usuario)
                : "Bienvenido, invitado");

        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile)   .setVisible(userReal);
        menu.findItem(R.id.nav_purchases) .setVisible(userReal);
        menu.findItem(R.id.nav_logout)    .setVisible(userReal);
        if (menu.findItem(R.id.nav_scan) != null)
            menu.findItem(R.id.nav_scan).setVisible(userReal);
        menu.findItem(R.id.nav_login)     .setVisible(!isLoggedIn || isGuest);
        navigationView.setCheckedItem(R.id.nav_about);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            item.setChecked(true);

            /* Bloqueo selectivo para invitados / sin sesión */
            if (!userReal) {
                if (id == R.id.nav_profile ||
                        id == R.id.nav_purchases ||
                        id == R.id.nav_logout ||
                        id == R.id.nav_scan) {
                    startActivity(new Intent(this, Login2.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            }

            if      (id == R.id.nav_home)        { startActivity(new Intent(this, MainActivity.class)); finish(); }
            else if (id == R.id.nav_logout)   { prefs.edit().clear().apply(); startActivity(new Intent(this, Login2.class)); finish(); }
            else if (id == R.id.nav_login)       startActivity(new Intent(this, Login2.class));
            else if (id == R.id.nav_profile)     startActivity(new Intent(this, VerPerfil.class));
            else if (id == R.id.nav_faq)         startActivity(new Intent(this, PreguntasFrecuentes.class));
            else if (id == R.id.nav_purchases)   startActivity(new Intent(this, MisCompras.class));
            else if (id == R.id.nav_promotions)  startActivity(new Intent(this, Promociones.class));
            else if (id == R.id.nav_branches)    startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_perfumes)    startActivity(new Intent(this, Perfumes.class));
            else if (id == R.id.nav_scan)        startActivity(new Intent(this, ScanUPCActivity.class));
            /* nav_about = pantalla actual */

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    /* --------------------------------------------------- */
    @Override
    protected void onResume() {
        super.onResume();
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST , false);
        configurarDrawer(prefs.getString(KEY_USERNAME, ""));
    }

    /* --------------------------------------------------- */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }
}
