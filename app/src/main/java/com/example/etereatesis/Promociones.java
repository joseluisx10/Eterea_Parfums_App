package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.etereatesis.adaptadores.PromotionsAdapter;
import com.example.etereatesis.models.ComboItem;
import com.example.etereatesis.models.Perfume;
import com.google.android.material.navigation.NavigationView;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.relex.circleindicator.CircleIndicator3;

public class Promociones extends AppCompatActivity {

    /* ---------- Const ---------- */
    private static final String TAG            = "PROMO_DEBUG";
    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;
    private static final String PREF_NAME      = "LoginPrefs";
    private static final String KEY_LOGGED_IN  = "isLoggedIn";
    private static final String KEY_IS_GUEST   = "isGuest";
    private static final String KEY_USERNAME   = "username";

    /* ---------- Sesión ---------- */
    private SharedPreferences prefs;
    private boolean isLogged, isGuest;

    /* ---------- UI ---------- */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;

    private ViewPager2            viewPager;
    private ImageButton           btnLeftPromo, btnRightPromo;
    private CircleIndicator3      circleIndicator;

    private RecyclerView          recyclerGrid;
    private PromotionsAdapter     adapter;
    private final List<Perfume>   listaPromociones = new ArrayList<>();

    /* ---------- Moneda ---------- */
    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    /* ======================================================= */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promociones);

        /* --- Sesión --- */
        prefs    = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLogged = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest  = prefs.getBoolean(KEY_IS_GUEST , false);

        /* --- Toolbar + Drawer --- */
        Toolbar toolbar = findViewById(R.id.toolbarPromociones);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView = findViewById(R.id.navigation_view);
        configurarDrawer();

        /* --- Carrusel (opcional, se muestra solo si lo llenas) --- */
        viewPager       = findViewById(R.id.carouselPromociones);
        btnLeftPromo    = findViewById(R.id.btnLeftPromo);
        btnRightPromo   = findViewById(R.id.btnRightPromo);
        circleIndicator = findViewById(R.id.circleIndicatorProm);
        circleIndicator.setViewPager(viewPager);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        btnLeftPromo.setOnClickListener(v -> {
            int prev = viewPager.getCurrentItem() - 1;
            if (prev >= 0) viewPager.setCurrentItem(prev, true);
        });
        btnRightPromo.setOnClickListener(v -> {
            int next = viewPager.getCurrentItem() + 1;
            if (viewPager.getAdapter() != null
                    && next < viewPager.getAdapter().getItemCount()) {
                viewPager.setCurrentItem(next, true);
            }
        });

        /* --- Grid de promociones --- */
        recyclerGrid = findViewById(R.id.recyclerPromocionesGrid);
        recyclerGrid.setLayoutManager(new GridLayoutManager(this, 2));

        boolean userReal = isLogged && !isGuest;
        adapter = new PromotionsAdapter(
                listaPromociones,
                userReal,
                this::agregarAlCarrito,
                this::restarDelCarrito
        );
        recyclerGrid.setAdapter(adapter);

        /* --- Datos iniciales --- */
        cargarPromociones();
        cargarCantidadesCarrito();
    }

    /* ========== MENÚ TOOLBAR (search, filtro, carrito) ========== */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Search
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        sv.setQueryHint("Buscar promociones…");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q)    {
                adapter.getFilter().filter(q);
                return false;
            }
            @Override public boolean onQueryTextChange(String txt) {
                adapter.getFilter().filter(txt);
                return false;
            }
        });

        // Carrito visible solo para usuarios logueados reales
        menu.findItem(R.id.action_cart).setVisible(isLogged && !isGuest);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;

        int id = item.getItemId();
        if (id == R.id.action_cart) {
            startActivity(new Intent(this, CarritoActivity.class));
            return true;
        }
        if (id == R.id.action_filter) {
            mostrarDialogoFiltros();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ================= Drawer ================= */
    private void configurarDrawer() {
        boolean userReal = isLogged && !isGuest;
        String usuario   = prefs.getString(KEY_USERNAME, "");

        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText(userReal
                ? "Bienvenido, " + usuario
                : "Bienvenido, invitado");

        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile)   .setVisible(userReal);
        menu.findItem(R.id.nav_purchases) .setVisible(userReal);
        menu.findItem(R.id.nav_logout)    .setVisible(userReal);
        if (menu.findItem(R.id.nav_scan) != null)
            menu.findItem(R.id.nav_scan).setVisible(userReal);
        menu.findItem(R.id.nav_login)     .setVisible(!isLogged || isGuest);

        navigationView.setCheckedItem(R.id.nav_promotions);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            item.setChecked(true);

            if (!userReal && (id == R.id.nav_profile || id == R.id.nav_purchases
                    || id == R.id.nav_logout || id == R.id.nav_scan)) {
                Toast.makeText(this, "Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login2.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_logout) {
                prefs.edit().clear().apply();
                startActivity(new Intent(this, Login2.class));
                finish();
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, Login2.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, VerPerfil.class));
            } else if (id == R.id.nav_faq) {
                startActivity(new Intent(this, PreguntasFrecuentes.class));
            } else if (id == R.id.nav_purchases) {
                startActivity(new Intent(this, MisCompras.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, SobreNosotros.class));
            } else if (id == R.id.nav_branches) {
                startActivity(new Intent(this, MapsActivity.class));
            } else if (id == R.id.nav_perfumes) {
                startActivity(new Intent(this, Perfumes.class));
            } else if (id == R.id.nav_scan) {
                startActivity(new Intent(this, ScanUPCActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    /* ================= Promociones ================= */
    private void cargarPromociones() {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                     CallableStatement cs = cn.prepareCall("{ call sp_ObtenerPerfumesPromocion }");
                     ResultSet rs = cs.executeQuery()) {

                    listaPromociones.clear();
                    while (rs.next()) {
                        Perfume p = new Perfume(
                                rs.getInt("id"),
                                rs.getString("nombre_perfume"),
                                rs.getDouble("precio_promocion"),
                                rs.getString("imagen_ruta"),
                                rs.getString("codigo_upc"),
                                rs.getString("descripcion"),
                                rs.getInt("presentacion_ml"),
                                rs.getString("nombre_marca")
                        );
                        p.setEnPromocion(true);
                        p.setPrecioOriginal  (rs.getDouble("precio_original"));
                        p.setPrecioPromocion(rs.getDouble("precio_promocion"));
                        listaPromociones.add(p);
                    }
                }

                runOnUiThread(() -> {
                    adapter.updateData(listaPromociones);
                    if (listaPromociones.isEmpty())
                        Toast.makeText(this,"No hay promociones disponibles",
                                Toast.LENGTH_SHORT).show();
                    cargarCantidadesCarrito();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error cargando promociones", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Error al cargar promociones: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /* ================= Cantidades existentes ================= */
    private void cargarCantidadesCarrito() {
        if (!isLogged || isGuest) return;
        int clientId = prefs.getInt("clientId", -1);
        if (clientId < 0) return;

        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_ObtenerCarrito(?) }")) {

                cs.setInt(1, clientId);
                ResultSet rs = cs.executeQuery();

                Map<Integer,Integer> map = new HashMap<>();
                while (rs.next()) {
                    map.put(rs.getInt("perfume_id"), rs.getInt("cantidad"));
                }
                runOnUiThread(() -> {
                    for (Perfume p : listaPromociones)
                        p.setCantidad(map.getOrDefault(p.getId(), 0));
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e(TAG,"Cantidades carrito",e);
            }
        }).start();
    }

    /* ================= Carrito ± ================= */
    private void agregarAlCarrito(Perfume p) {
        if (!isLogged || isGuest) {
            startActivity(new Intent(this, Login2.class));
            return;
        }
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs =
                         cn.prepareCall("{ call sp_AgregarCarrito(?, ?, ?) }")) {
                cs.setInt(1, p.getId());
                cs.setInt(2, prefs.getInt("clientId", -1));
                cs.setInt(3, 1);
                cs.execute();
            } catch (Exception e) {
                Log.e(TAG,"Add carrito",e);
            }
        }).start();
    }

    private void restarDelCarrito(Perfume p) {
        if (!isLogged || isGuest) return;
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs =
                         cn.prepareCall("{ call sp_RestarCarrito(?, ?, ?) }")) {
                cs.setInt(1, p.getId());
                cs.setInt(2, prefs.getInt("clientId", -1));
                cs.registerOutParameter(3, java.sql.Types.INTEGER);
                cs.execute();
            } catch (Exception e) {
                Log.e(TAG,"Restar carrito",e);
            }
        }).start();
    }

    /* ========================= FILTROS ========================= */
    private void mostrarDialogoFiltros() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View dlg = LayoutInflater.from(this).inflate(R.layout.filter_dialog, null);
        b.setView(dlg);
        AlertDialog dialog = b.create();

        Spinner      spM  = dlg.findViewById(R.id.spMarca);
        Spinner      spG  = dlg.findViewById(R.id.spGenero);
        Spinner      spT  = dlg.findViewById(R.id.spTipoPerfume);
        Spinner      spA  = dlg.findViewById(R.id.spFamiliaOlfativa);
        LinearLayout llNotas = dlg.findViewById(R.id.llNotasContainer);
        EditText     etMin = dlg.findViewById(R.id.etPrecioMin);
        EditText     etMax = dlg.findViewById(R.id.etPrecioMax);
        EditText     etTmin= dlg.findViewById(R.id.etTamanioMin);
        EditText     etTmax= dlg.findViewById(R.id.etTamanioMax);
        Button       btnApp= dlg.findViewById(R.id.btnAplicarFiltros);
        Button       btnClr= dlg.findViewById(R.id.btnLimpiarFiltros);

        cargarSpinnerData("sp_GetMarcas",         spM);
        cargarSpinnerData("sp_GetGeneros",        spG);
        cargarSpinnerData("sp_GetTiposDePerfume", spT);
        cargarSpinnerData("sp_GetAromas",         spA);
        cargarNotasCheckBoxes(llNotas);

        btnApp.setOnClickListener(v -> {
            /* 1. IDs o NULL */
            Integer mId = idOrNull((ComboItem) spM.getSelectedItem());
            Integer gId = idOrNull((ComboItem) spG.getSelectedItem());
            Integer tId = idOrNull((ComboItem) spT.getSelectedItem());
            Integer aId = idOrNull((ComboItem) spA.getSelectedItem());

            /* 2. Rango precios y tamaños */
            Double  pMin = parseDoubleOrNull(etMin);
            Double  pMax = parseDoubleOrNull(etMax);
            Integer tMin = parseIntOrNull  (etTmin);
            Integer tMax = parseIntOrNull  (etTmax);

            /* 3. CSV de notas elegidas */
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < llNotas.getChildCount(); i++) {
                View c = llNotas.getChildAt(i);
                if (c instanceof CheckBox && ((CheckBox) c).isChecked()) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(c.getTag());
                }
            }
            String notaCsv = sb.length() > 0 ? sb.toString() : null;

            cargarPromosFiltradas(mId, gId, tId,
                    pMin, pMax,
                    tMin, tMax,
                    aId, notaCsv);
            dialog.dismiss();
        });

        btnClr.setOnClickListener(v -> {
            cargarPromociones();        // sin filtros
            dialog.dismiss();
        });

        dialog.show();
    }

    /* ---------- Helpers comunes ---------- */
    private Integer idOrNull(ComboItem c) { return (c.getId() == 0) ? null : c.getId(); }
    private Double  parseDoubleOrNull(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? null : Double.parseDouble(s);
    }
    private Integer parseIntOrNull(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? null : Integer.parseInt(s);
    }

    private void cargarSpinnerData(String spName, Spinner spinner) {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall("{ call " + spName + " }");
                ResultSet rs = cs.executeQuery();

                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, "Todos"));

                while (rs.next()) {
                    items.add(new ComboItem(rs.getInt("id"), rs.getString("nombre")));
                }
                rs.close(); cs.close(); cn.close();

                runOnUiThread(() -> {
                    ArrayAdapter<ComboItem> adp = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            items);
                    adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adp);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error al cargar datos: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void cargarNotasCheckBoxes(LinearLayout container) {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall("{ call sp_GetNotas }");
                ResultSet rs = cs.executeQuery();

                List<ComboItem> notas = new ArrayList<>();
                while (rs.next()) {
                    notas.add(new ComboItem(rs.getInt("id"), rs.getString("nombre")));
                }
                rs.close(); cs.close(); cn.close();

                runOnUiThread(() -> {
                    container.removeAllViews();
                    for (ComboItem n : notas) {
                        CheckBox cb = new CheckBox(this);
                        cb.setText(n.toString());
                        cb.setTag(n.getId());
                        container.addView(cb);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al cargar notas", e);
            }
        }).start();
    }

    private void cargarPromosFiltradas(
            Integer marcaId,
            Integer generoId,
            Integer tipoPerfumeId,
            Double  precioMin,
            Double  precioMax,
            Integer tamanioMin,
            Integer tamanioMax,
            Integer aromaId,
            String  notaCsv
    ) {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall(
                        "{ call sp_ObtenerPerfumesFiltrados(?, ?, ?, ?, ?, ?, ?, ?, ?) }");

                /* Ints --------------------------------------------------- */
                if (marcaId  != null) cs.setInt(1, marcaId);  else cs.setNull(1, java.sql.Types.INTEGER);
                if (generoId != null) cs.setInt(2, generoId); else cs.setNull(2, java.sql.Types.INTEGER);
                if (tipoPerfumeId!=null)cs.setInt(3,tipoPerfumeId);else cs.setNull(3, java.sql.Types.INTEGER);

                /* Decimals ---------------------------------------------- */
                if (precioMin != null) cs.setBigDecimal(4, BigDecimal.valueOf(precioMin));
                else                   cs.setNull(4, java.sql.Types.DECIMAL);

                if (precioMax != null) cs.setBigDecimal(5, BigDecimal.valueOf(precioMax));
                else                   cs.setNull(5, java.sql.Types.DECIMAL);

                /* Tamaños ----------------------------------------------- */
                if (tamanioMin != null) cs.setInt(6, tamanioMin);
                else                     cs.setNull(6, java.sql.Types.INTEGER);
                if (tamanioMax != null) cs.setInt(7, tamanioMax);
                else                     cs.setNull(7, java.sql.Types.INTEGER);

                /* Aroma -------------------------------------------------- */
                if (aromaId != null) cs.setInt(8, aromaId);
                else                  cs.setNull(8, java.sql.Types.INTEGER);

                /* Notas CSV --------------------------------------------- */
                if (notaCsv != null) cs.setString(9, notaCsv);
                else                  cs.setNull(9, java.sql.Types.NVARCHAR);

                ResultSet rs = cs.executeQuery();
                List<Perfume> filtered = new ArrayList<>();
                while (rs.next()) {
                    filtered.add(new Perfume(
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
                rs.close(); cs.close(); cn.close();

                runOnUiThread(() -> {
                    adapter.updateData(filtered);
                    if (filtered.isEmpty()) {
                        Toast.makeText(this,
                                "No se encontraron resultados con esos filtros",
                                Toast.LENGTH_SHORT).show();
                    }
                    cargarCantidadesCarrito();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error en cargarPromosFiltradas", e);
            }
        }).start();
    }

    /* ================= Ciclo ================= */
    @Override
    protected void onResume() {
        super.onResume();
        isLogged = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest  = prefs.getBoolean(KEY_IS_GUEST , false);
        configurarDrawer();
        cargarCantidadesCarrito();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }
}
