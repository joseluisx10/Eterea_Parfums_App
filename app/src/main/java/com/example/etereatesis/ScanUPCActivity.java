package com.example.etereatesis;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.etereatesis.models.Perfume;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ScanUPCActivity extends AppCompatActivity {

    /* ========= CONST ========= */
    private static final String TAG                     = "ScanUPCActivity";
    private static final int    REQUEST_CAMERA_PERMISSION = 100;
    private static final String PREF_NAME               = "LoginPrefs";
    private static final String KEY_LOGGED_IN           = "isLoggedIn";
    private static final String KEY_IS_GUEST            = "isGuest";
    private static final String KEY_USERNAME            = "username";

    /* ========= UI ========= */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;
    private ProgressBar           progressBar;
    private TextView              txtLoadingMessage;

    /* ========= SESIÓN ========= */
    private SharedPreferences prefs;
    private boolean isLoggedIn, isGuest, userReal;

    /* ========= DATA ========= */
    private final List<Perfume> perfumeList = new ArrayList<>();

    /* ----------------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* ---------- Sesión ---------- */
        prefs      = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST , false);
        userReal   = isLoggedIn && !isGuest;             // PATCH

        // 🔐 Solo usuarios reales pueden usar el escáner
        if (!userReal) {
            startActivity(new Intent(this, Login2.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_scan_upcactivity);

        String usuario = prefs.getString(KEY_USERNAME, "");

        /* ---------- Toolbar & Drawer ---------- */
        Toolbar toolbar = findViewById(R.id.toolbarScanUPC);
        setSupportActionBar(toolbar);

        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle   = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        configurarDrawer(usuario);   // PATCH

        /* ---------- Vistas de carga ---------- */
        progressBar       = findViewById(R.id.progressBarLoading);
        txtLoadingMessage = findViewById(R.id.txtLoadingMessage);

        cargarPerfumes();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

    }

    /* ================= Drawer ================= */
    private void configurarDrawer(String usuario) {
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText("Bienvenido, " + usuario);

        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile)   .setVisible(true);
        menu.findItem(R.id.nav_purchases) .setVisible(true);
        menu.findItem(R.id.nav_logout)    .setVisible(true);
        // nav_scan es la pantalla actual: deja visible y marcada
        navigationView.setCheckedItem(R.id.nav_scan);
        // Login oculto porque userReal
        menu.findItem(R.id.nav_login).setVisible(false);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            item.setChecked(true);

            if (id == R.id.nav_home)        { startActivity(new Intent(this, MainActivity.class)); finish(); }
            else if (id == R.id.nav_logout) { prefs.edit().clear().apply(); startActivity(new Intent(this, Login2.class)); finish(); }
            else if (id == R.id.nav_profile)     startActivity(new Intent(this, VerPerfil.class));
            else if (id == R.id.nav_faq)         startActivity(new Intent(this, PreguntasFrecuentes.class));
            else if (id == R.id.nav_purchases)   startActivity(new Intent(this, MisCompras.class));
            else if (id == R.id.nav_about)       startActivity(new Intent(this, SobreNosotros.class));
            else if (id == R.id.nav_promotions)  startActivity(new Intent(this, Promociones.class));
            else if (id == R.id.nav_branches)    startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_perfumes)    startActivity(new Intent(this, Perfumes.class));
            /* nav_scan = pantalla actual */
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    /* ----------------------------------------------------------- */
    private void cargarPerfumes() {
        progressBar.setVisibility(View.VISIBLE);
        txtLoadingMessage.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DETECTOR.CONNECTION_URL);
                     CallableStatement stmt = conn.prepareCall("{ call ObtenerPerfumes }");
                     ResultSet rs = stmt.executeQuery()) {

                    perfumeList.clear();
                    while (rs.next()) {
                        perfumeList.add(new Perfume(
                                rs.getInt("id"),
                                rs.getString("nombre_perfume"),
                                rs.getDouble("precio_en_pesos"),
                                rs.getString("imagen1"),
                                rs.getString("codigo_upc"),
                                rs.getString("descripcion"),
                                rs.getInt("presentacion_ml"),
                                rs.getString("nombre_marca")
                        ));
                    }
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    txtLoadingMessage.setVisibility(View.GONE);
                    if (perfumeList.isEmpty()) {
                        Toast.makeText(this, "No se encontraron perfumes", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        checkCameraPermissionAndScan();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error cargando perfumes", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión o lectura", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    /* ----------------------------------------------------------- */
    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            startBarcodeScanner();
        }
    }

    private void startBarcodeScanner() {
        new IntentIntegrator(this)
                .setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
                .setPrompt("Escanea el código de barras")
                .setBeepEnabled(true)
                .setOrientationLocked(true)
                .initiateScan();
    }

    /* ----------------------------------------------------------- */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBarcodeScanner();
        } else {
            Toast.makeText(this, "Permiso de cámara rechazado", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                finish();
            } else {
                Perfume encontrado = findPerfumeByUPC(result.getContents());
                if (encontrado != null) {
                    navigateToDetail(encontrado);
                } else {
                    Toast.makeText(this, "Producto no encontrado: " + result.getContents(), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /* ----------------------------------------------------------- */
    private Perfume findPerfumeByUPC(String upc) {
        for (Perfume p : perfumeList) {
            if (upc.equals(p.getVarCodeUPC())) return p;
        }
        return null;
    }

    private void navigateToDetail(Perfume p) {
        Intent i = new Intent(this, DetalleProducto.class);
        i.putExtra("id",             p.getId());
        i.putExtra("nombre",         p.getNombre());
        i.putExtra("precio",         p.getPrecio());
        i.putExtra("imagenRuta",     p.getImagenRuta());
        i.putExtra("codigo_upc",     p.getVarCodeUPC());
        i.putExtra("descripcion",    p.getDescripcion());
        i.putExtra("presentacionML", p.getPresentacionML());
        i.putExtra("marca",          p.getNombreMarca());
        startActivity(i);
        finish();
    }

    /* ----------------------------------------------------------- */
    @Override
    protected void onResume() {
        super.onResume();
        // Si por alguna razón pierde la sesión mientras está abierta
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST , false);
        if (!isLoggedIn || isGuest) {
            startActivity(new Intent(this, Login2.class));
            finish();
        }
    }

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
