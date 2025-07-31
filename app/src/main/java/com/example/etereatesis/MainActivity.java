package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.etereatesis.adaptadores.BannerAdapter;
import com.example.etereatesis.adaptadores.CarouselAdapter;
import com.example.etereatesis.adaptadores.PerfumeAdapter;
import com.example.etereatesis.models.Banner;
import com.example.etereatesis.models.ComboItem;
import com.example.etereatesis.models.Perfume;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DB_DEBUG";
    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST = "isGuest";

    private RecyclerView recyclerPerfumes;
    private ViewPager2 carouselPerfumes;
    private CircleIndicator3 circleIndicator;
    private List<Perfume> listaPerfumes = new ArrayList<>();
    private PerfumeAdapter adapter;
    private CarouselAdapter carouselAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private SharedPreferences sharedPreferences;
    private boolean isLoggedIn;
    boolean isGuest;
    private ViewPager2       carouselBanners;
    private CircleIndicator3 indicatorBanners;
    private BannerAdapter bannerAdapter;
    private final List<Banner> listaBanners = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* ① SIEMPRE llama primero a super */
        super.onCreate(savedInstanceState);

        /* ② Control de sesión (logueado real / invitado / anónimo) */
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isLogged = sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
        boolean isGuest  = sharedPreferences.getBoolean(KEY_IS_GUEST , false);

        // Si NO hay sesión de ningún tipo → pantalla de ingreso
        if (!isLogged && !isGuest) {
            startActivity(new Intent(this, Login2.class));
            finish();
            return;
        }

        /* ③ Cargar UI */
        setContentView(R.layout.activity_main);

        /* ---------- Estado de la instancia ---------- */
        this.isLoggedIn = isLogged;
        this.isGuest    = isGuest;
        String usuario  = sharedPreferences.getString(KEY_USERNAME, "Invitado");

        /* ---------- Toolbar ---------- */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        /* ---------- Recycler y carrusel ---------- */
        recyclerPerfumes = findViewById(R.id.recyclerPerfumes);
        recyclerPerfumes.setLayoutManager(new GridLayoutManager(this, 2));

        carouselPerfumes = findViewById(R.id.carouselPerfumes);
        circleIndicator  = findViewById(R.id.circleIndicator);

        adapter = new PerfumeAdapter(
                listaPerfumes,
                /* canAdd */ isLoggedIn && !isGuest,
                this::agregarAlCarrito,
                this::restarDelCarrito);
        recyclerPerfumes.setAdapter(adapter);

        carouselAdapter = new CarouselAdapter(
                listaPerfumes, p -> Log.d(TAG, "Perfume seleccionado: " + p.getNombre()));
        carouselPerfumes.setAdapter(carouselAdapter);
        circleIndicator.setViewPager(carouselPerfumes);

        ImageButton btnLeft  = findViewById(R.id.btnLeftArrow);
        ImageButton btnRight = findViewById(R.id.btnRightArrow);
        btnLeft .setOnClickListener(v -> {
            int i = carouselPerfumes.getCurrentItem();
            if (i > 0) carouselPerfumes.setCurrentItem(i - 1, true);
        });
        btnRight.setOnClickListener(v -> {
            int i = carouselPerfumes.getCurrentItem();
            if (i < carouselAdapter.getItemCount() - 1)
                carouselPerfumes.setCurrentItem(i + 1, true);
        });

        /* ---------- Drawer & NavigationView ---------- */
        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle   = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText("Bienvenido, " + usuario);

        Menu nav = navigationView.getMenu();
        nav.findItem(R.id.nav_profile)   .setVisible(isLoggedIn && !isGuest);
        nav.findItem(R.id.nav_purchases) .setVisible(isLoggedIn && !isGuest);
        nav.findItem(R.id.nav_logout)    .setVisible(isLoggedIn && !isGuest);
        nav.findItem(R.id.nav_login)     .setVisible(!isLoggedIn);  // se muestra a invitados/anónimos

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // Invitado: bloquear pantallas privadas
            if (isGuest && (id == R.id.nav_profile
                    || id == R.id.nav_purchases
                    || id == R.id.nav_logout)) {
                Toast.makeText(this, "Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login2.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            /* Navegación a las diferentes pantallas */
            if      (id == R.id.nav_login)       startActivity(new Intent(this, Login2.class));
            else if (id == R.id.nav_logout)      cerrarSesion();
            else if (id == R.id.nav_profile)     startActivity(new Intent(this, VerPerfil.class));
            else if (id == R.id.nav_faq)         startActivity(new Intent(this, PreguntasFrecuentes.class));
            else if (id == R.id.nav_purchases)   startActivity(new Intent(this, MisCompras.class));
            else if (id == R.id.nav_about)       startActivity(new Intent(this, SobreNosotros.class));
            else if (id == R.id.nav_promotions)  startActivity(new Intent(this, Promociones.class));
            else if (id == R.id.nav_branches)    startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_perfumes)    startActivity(new Intent(this, Perfumes.class));
            else if (id == R.id.nav_scan)        startActivity(new Intent(this, ScanUPCActivity.class));

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        /* ---------- Carrusel de banners ---------- */
        carouselBanners  = findViewById(R.id.carouselBanners);
        indicatorBanners = findViewById(R.id.indicatorBanners);
        bannerAdapter = new BannerAdapter(
                listaBanners,
                b -> {
                    // Solo si el banner apunta a un perfume
                    if (b.getPerfumeId() > 0) {
                        Intent i = new Intent(this, DetalleProducto.class);

                        /* --- Extras que DetalleProducto ya espera --- */
                        i.putExtra("id",            b.getPerfumeId());
                        i.putExtra("nombre",        b.getNombrePerfume());
                        i.putExtra("precio",        b.getPrecioPromo());
                        i.putExtra("imagenRuta",    b.getImagenRuta());
                        i.putExtra("descripcion",   b.getDescripcion());
                        i.putExtra("presentacionML",      b.getPresentacionMl());
                        i.putExtra("marca",               b.getNombreMarca());
                        i.putExtra("cantidad",      1);          // cantidad inicial

                        startActivity(i);
                    }
                });

        carouselBanners.setAdapter(bannerAdapter);
        indicatorBanners.setViewPager(carouselBanners);

        /* ---------- Cargar datos ---------- */
        cargarPerfumes();
        cargarBanners();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Carrito
        MenuItem cart = menu.findItem(R.id.action_cart);
        if (cart != null) cart.setVisible(isLoggedIn && !isGuest);
        // Search
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView sv = (SearchView) searchItem.getActionView();
            sv.setQueryHint("Buscar perfumes…");
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
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cart) {
            if (!isLoggedIn || isGuest) {
                startActivity(new Intent(this, Login2.class));
            } else {
                startActivity(new Intent(this, CarritoActivity.class));
            }
            return true;
        }

        if (id == R.id.action_filter) {
            mostrarDialogoFiltros();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesion() {
        sharedPreferences.edit().clear().apply();
        startActivity(new Intent(this, Login2.class));
        finish();
    }

    private void cargarBanners() {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall("{ call sp_ObtenerPerfumesPromocion }");
                ResultSet rs = cs.executeQuery();

                List<Banner> tmp = new ArrayList<>();
                while (rs.next()) {
                    tmp.add(new Banner(
                            /* id fila tab. promo  */ rs.getInt("id"),        // <- usa alias del SELECT
                            /* perfumeId destino  */ rs.getInt("id"),        // mismo id de perfume
                            /* imagen              */ rs.getString("imagen_ruta"),
                            /* nombre perfume      */ rs.getString("nombre_perfume"),
                            /* precio con desc.    */ rs.getDouble("precio_promocion"),
                            /* descripción         */ rs.getString("descripcion"),
                            /* presentación (ml)   */ rs.getInt("presentacion_ml"),
                            /* marca               */ rs.getString("nombre_marca")
                    ));
                }
                rs.close(); cs.close(); cn.close();

                runOnUiThread(() -> bannerAdapter.updateData(tmp));

            } catch (Exception e) {
                Log.e(TAG, "Error cargando banners", e);
            }
        }).start();
    }


    private void cargarPerfumes() {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall("{ call ObtenerPerfumes }");
                ResultSet rs = cs.executeQuery();

                listaPerfumes.clear();
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
                    listaPerfumes.add(p);
                }
                rs.close();
                cs.close();
                cn.close();

                runOnUiThread(() -> {
                    adapter.updateData(listaPerfumes);
                    carouselAdapter.updateData(listaPerfumes);
                    // Cargar las cantidades actuales del carrito
                    cargarCantidadesCarrito();
                });
            } catch (Exception e) {
                Log.e(TAG, "ERROR en la conexión o ejecución.", e);
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error al cargar perfumes: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void cargarCantidadesCarrito() {
        int clientId = sharedPreferences.getInt("clientId", -1);
        if (clientId < 0) return;

        new Thread(() -> {
            try {
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall("{ call sp_ObtenerCarrito(?) }");
                cs.setInt(1, clientId);
                ResultSet rs = cs.executeQuery();

                Map<Integer,Integer> map = new HashMap<>();
                while (rs.next()) {
                    map.put(rs.getInt("perfume_id"), rs.getInt("cantidad"));
                }
                rs.close();
                cs.close();
                cn.close();

                runOnUiThread(() -> {
                    for (Perfume p : listaPerfumes) {
                        p.setCantidad(map.getOrDefault(p.getId(), 0));
                    }
                    adapter.updateData(listaPerfumes);

                });
            } catch (Exception e) {
                Log.e(TAG, "Error en cargarCantidadesCarrito", e);
            }
        }).start();
    }

    private void restarDelCarrito(Perfume perfume) {
        int clientId = sharedPreferences.getInt("clientId", -1);
        if (clientId < 0) return;

        //--- Evita doble click rápido

        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_RestarCarrito(?, ?, ?) }")) {

                cs.setInt(1, perfume.getId());
                cs.setInt(2, clientId);
                cs.registerOutParameter(3, java.sql.Types.INTEGER);
                cs.execute();

                int res = cs.getInt(3);

                runOnUiThread(() -> {
                    // ‼️ NO hagas perfume.setCantidad(…);
                    if (res == 1) {
                        cargarCantidadesCarrito();   // una sola resta — viene de la BD
                    } else {
                        Toast.makeText(this, "No había nada para quitar", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error al quitar: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }



    private void agregarAlCarrito(Perfume perfume) {
        int clientId = sharedPreferences.getInt("clientId", -1);
        if (clientId < 0) {
            Toast.makeText(this,
                    "Error: No se encontró el ID del cliente",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_AgregarCarrito(?, ?, ?) }")) {

                cs.setInt(1, perfume.getId());
                cs.setInt(2, clientId);
                cs.setInt(3, 1); // cantidad fija a agregar
                cs.execute();

                runOnUiThread(() -> {
                    // Mensaje de confirmación
                    Toast.makeText(this,
                            "Perfume agregado al carrito",
                            Toast.LENGTH_SHORT).show();
                    // <-- Aquí recargamos las cantidades en pantalla
                    cargarCantidadesCarrito();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error al agregar al carrito: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }


    /* ========= 1) DIÁLOGO DE FILTROS ============ */
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

            /* 2. Rango precios y tamaños – pueden ser null */
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

            cargarPerfumesFiltrados(mId, gId, tId,
                    pMin, pMax,
                    tMin, tMax,
                    aId, notaCsv);
            dialog.dismiss();
        });

        btnClr.setOnClickListener(v -> {
            cargarPerfumes();        // sin filtros
            dialog.dismiss();
        });

        dialog.show();
    }

    /* Helpers */
    private Integer idOrNull(ComboItem c) { return (c.getId() == 0) ? null : c.getId(); }
    private Double  parseDoubleOrNull(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? null : Double.parseDouble(s);
    }
    private Integer parseIntOrNull(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? null : Integer.parseInt(s);
    }
    /* ========= 2) CARGAR DATOS EN SPINNER ============ */
    private void cargarSpinnerData(String spName, Spinner spinner) {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement cs = cn.prepareCall("{ call " + spName + " }");
                ResultSet rs = cs.executeQuery();

                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, "Todos"));   // ← opción “Todos”

                while (rs.next()) {
                    items.add(new ComboItem(
                            rs.getInt("id"),
                            rs.getString("nombre")
                    ));
                }
                rs.close();
                cs.close();
                cn.close();

                runOnUiThread(() -> {
                    ArrayAdapter<ComboItem> adp = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            items
                    );
                    adp.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    spinner.setAdapter(adp);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error al cargar datos: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
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
                    notas.add(new ComboItem(
                            rs.getInt("id"),
                            rs.getString("nombre")
                    ));
                }
                rs.close();
                cs.close();
                cn.close();

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

    /* ========= 2) LLAMAR AL SP (corregido) ============ */
    private void cargarPerfumesFiltrados(
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

                /* ------------ parámetros numéricos ------------ */
                if (marcaId       != null) cs.setInt(1, marcaId);       else cs.setNull(1, java.sql.Types.INTEGER);
                if (generoId      != null) cs.setInt(2, generoId);      else cs.setNull(2, java.sql.Types.INTEGER);
                if (tipoPerfumeId != null) cs.setInt(3, tipoPerfumeId); else cs.setNull(3, java.sql.Types.INTEGER);

                if (precioMin != null) cs.setBigDecimal(4, BigDecimal.valueOf(precioMin)); else cs.setNull(4, java.sql.Types.DECIMAL);
                if (precioMax != null) cs.setBigDecimal(5, BigDecimal.valueOf(precioMax)); else cs.setNull(5, java.sql.Types.DECIMAL);

                if (tamanioMin != null) cs.setInt(6, tamanioMin); else cs.setNull(6, java.sql.Types.INTEGER);
                if (tamanioMax != null) cs.setInt(7, tamanioMax); else cs.setNull(7, java.sql.Types.INTEGER);

                if (aromaId != null) cs.setInt(8, aromaId); else cs.setNull(8, java.sql.Types.INTEGER);

                /* ------------ CSV de notas ------------ */
                if (notaCsv == null || notaCsv.trim().isEmpty()) {
                    cs.setNull(9, java.sql.Types.VARCHAR);          // VARCHAR para evitar el error -9
                } else {
                    cs.setString(9, notaCsv);
                }

                /* ------------ ejecución ------------ */
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
                    /* ⇢ ACTUALIZA la lista global antes de refrescar cantidades */
                    listaPerfumes.clear();
                    listaPerfumes.addAll(filtered);

                    adapter.updateData(listaPerfumes);
                    carouselAdapter.updateData(listaPerfumes);

                    if (filtered.isEmpty()) {
                        Toast.makeText(this,
                                "No se encontraron perfumes con esos filtros",
                                Toast.LENGTH_SHORT).show();
                    }

                    /* usa la lista ya filtrada para aplicar las cantidades */
                    cargarCantidadesCarrito();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error en cargarPerfumesFiltrados", e);
            }
        }).start();
    }




    @Override
    protected void onResume() {
        super.onResume();
        isLoggedIn = sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = sharedPreferences.getBoolean(KEY_IS_GUEST, false);

        Menu navMenu = navigationView.getMenu();
        navMenu.findItem(R.id.nav_profile)   .setVisible(isLoggedIn && !isGuest);
        navMenu.findItem(R.id.nav_purchases) .setVisible(isLoggedIn && !isGuest);
        navMenu.findItem(R.id.nav_logout)    .setVisible(isLoggedIn && !isGuest);
        navMenu.findItem(R.id.nav_login)     .setVisible(!isLoggedIn || isGuest);
        cargarCantidadesCarrito();
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
