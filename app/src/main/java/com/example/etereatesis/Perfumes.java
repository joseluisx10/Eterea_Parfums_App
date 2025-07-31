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
import android.widget.ImageButton; // (si lo usas en layout; puedes eliminar)
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.adaptadores.PerfumeAdapter;
import com.example.etereatesis.models.ComboItem;
import com.example.etereatesis.models.Perfume;
import com.google.android.material.navigation.NavigationView;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Perfumes extends AppCompatActivity {

    /* --------- Constantes --------- */
    private static final String TAG            = "PERFUMES_DEBUG";
    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    private static final String PREF_NAME   = "LoginPrefs";
    private static final String KEY_LOGGED  = "isLoggedIn";
    private static final String KEY_GUEST   = "isGuest";
    private static final String KEY_USER    = "username";

    /* --------- UI --------- */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;

    private RecyclerView          rvPerfumes;
    private final List<Perfume>   listaPerfumes = new ArrayList<>();
    private PerfumeAdapter        adapter;

    /* --------- Sesión --------- */
    private SharedPreferences prefs;
    private boolean isLogged, isGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfumes);

        /* ---------- Sesión ---------- */
        prefs    = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLogged = prefs.getBoolean(KEY_LOGGED, false);
        isGuest  = prefs.getBoolean(KEY_GUEST , false);
        String usuario = prefs.getString(KEY_USER, "");

        /* ---------- Toolbar + Drawer ---------- */
        Toolbar tb = findViewById(R.id.toolbar_perfumes);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        drawerLayout   = findViewById(R.id.drawer_layout_perfumes);
        navigationView = findViewById(R.id.nav_view_perfumes);
        drawerToggle   = new ActionBarDrawerToggle(
                this, drawerLayout, tb,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        /* ---------- Header dinámico ---------- */
        View header = navigationView.getHeaderView(0);
        if (header == null) header = navigationView.inflateHeaderView(R.layout.drawer_header);

        TextView tvUser = header.findViewById(R.id.tv_user_name);
        tvUser.setText(isLogged && !isGuest
                ? "Bienvenido, " + usuario
                : "Bienvenido, invitado");

        /* ---------- Visibilidad de ítems ---------- */
        Menu m = navigationView.getMenu();
        boolean userReal = isLogged && !isGuest;
        m.findItem(R.id.nav_profile)   .setVisible(userReal);
        m.findItem(R.id.nav_purchases) .setVisible(userReal);
        m.findItem(R.id.nav_logout)    .setVisible(userReal);
        // PATCH: mostrar Login si invitado o sin sesión
        m.findItem(R.id.nav_login)     .setVisible(!isLogged || isGuest);
        // Si tienes nav_scan y quieres ocultarlo a invitados:
        if (m.findItem(R.id.nav_scan) != null) {
            m.findItem(R.id.nav_scan).setVisible(userReal);
        }
        m.findItem(R.id.nav_perfumes).setChecked(true);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            /* PATCH: bloqueo selectivo si no hay usuario real */
            if (!userReal) {
                if (id == R.id.nav_profile ||
                        id == R.id.nav_purchases ||
                        id == R.id.nav_logout ||
                        id == R.id.nav_scan /* si aplica */) {
                    Toast.makeText(this, "Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, Login2.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            }

            if      (id == R.id.nav_home)        startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_logout)   { prefs.edit().clear().apply(); startActivity(new Intent(this, Login2.class)); finish(); }
            else if (id == R.id.nav_login)       startActivity(new Intent(this, Login2.class));
            else if (id == R.id.nav_profile)     startActivity(new Intent(this, VerPerfil.class));
            else if (id == R.id.nav_purchases)   startActivity(new Intent(this, MisCompras.class));
            else if (id == R.id.nav_faq)         startActivity(new Intent(this, PreguntasFrecuentes.class));
            else if (id == R.id.nav_about)       startActivity(new Intent(this, SobreNosotros.class));
            else if (id == R.id.nav_promotions)  startActivity(new Intent(this, Promociones.class));
            else if (id == R.id.nav_branches)    startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_scan)        startActivity(new Intent(this, ScanUPCActivity.class));
            /* nav_perfumes = pantalla actual */

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        /* ---------- Recycler ---------- */
        rvPerfumes = findViewById(R.id.recyclerPerfumes);
        rvPerfumes.setLayoutManager(new GridLayoutManager(this, 2));

        boolean canAdd = userReal;
        adapter = new PerfumeAdapter(
                listaPerfumes,
                canAdd,
                this::agregarAlCarrito,
                this::restarDelCarrito
        );
        rvPerfumes.setAdapter(adapter);

        /* ---------- Carga inicial ---------- */
        cargarPerfumes();
        cargarCantidadesCarrito();
    }

    /* ===== Toolbar ===== */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Search
        SearchView sv = (SearchView) menu.findItem(R.id.action_search).getActionView();
        sv.setQueryHint("Buscar perfumes…");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q)  { adapter.getFilter().filter(q);  return false; }
            @Override public boolean onQueryTextChange(String t) { adapter.getFilter().filter(t);  return false; }
        });

        // Carrito sólo usuario real
        menu.findItem(R.id.action_cart).setVisible(isLogged && !isGuest);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;

        int id = item.getItemId();
        if (id == R.id.action_cart) {
            if (isLogged && !isGuest) {
                startActivity(new Intent(this, CarritoActivity.class));
            } else {
                startActivity(new Intent(this, Login2.class));
            }
            return true;
        } else if (id == R.id.action_filter) {
            mostrarDialogoFiltros();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ===== Carga de perfumes ===== */
    private void cargarPerfumes() {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                     CallableStatement cs = cn.prepareCall("{ call ObtenerPerfumes }");
                     ResultSet rs = cs.executeQuery()) {

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
                }

                runOnUiThread(() -> adapter.updateData(listaPerfumes));

            } catch (Exception e) {
                Log.e(TAG, "Error cargar perfumes", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "No se pudieron cargar perfumes", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    /* ===== Cantidades del carrito ===== */
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
                    for (Perfume p : listaPerfumes)
                        p.setCantidad(map.getOrDefault(p.getId(),0));
                    adapter.updateData(listaPerfumes);
                });
            } catch (Exception e) {
                Log.e(TAG,"Carrito error",e);
            }
        }).start();
    }

    /* ===== Diálogo de filtros ===== */
    private void mostrarDialogoFiltros() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.filter_dialog,null);
        b.setView(v);
        AlertDialog d = b.create();

        Spinner  spM = v.findViewById(R.id.spMarca);
        Spinner  spG = v.findViewById(R.id.spGenero);
        Spinner  spT = v.findViewById(R.id.spTipoPerfume);
        Spinner  spA = v.findViewById(R.id.spFamiliaOlfativa);
        LinearLayout llNotas = v.findViewById(R.id.llNotasContainer);
        EditText etMin = v.findViewById(R.id.etPrecioMin);
        EditText etMax = v.findViewById(R.id.etPrecioMax);
        Button   btnOK = v.findViewById(R.id.btnAplicarFiltros);
        Button   btnClr= v.findViewById(R.id.btnLimpiarFiltros);

        cargarSpinner("sp_GetMarcas",  spM);
        cargarSpinner("sp_GetGeneros", spG);
        cargarSpinner("sp_GetTiposDePerfume", spT);
        cargarSpinner("sp_GetAromas",  spA);
        cargarNotasCheck(llNotas);

        btnOK.setOnClickListener(x -> {
            Integer mId = idOrNull((ComboItem)spM.getSelectedItem());
            Integer gId = idOrNull((ComboItem)spG.getSelectedItem());
            Integer tId = idOrNull((ComboItem)spT.getSelectedItem());
            Integer aId = idOrNull((ComboItem)spA.getSelectedItem());
            Double  pMin= numOrNull(etMin);
            Double  pMax= numOrNull(etMax);

            // CSV notas
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<llNotas.getChildCount();i++) {
                View c = llNotas.getChildAt(i);
                if (c instanceof CheckBox && ((CheckBox)c).isChecked()) {
                    if (sb.length()>0) sb.append(',');
                    sb.append(c.getTag());
                }
            }
            String notasCsv = sb.length()>0 ? sb.toString() : null;

            cargarPerfumesFiltrados(mId,gId,tId,pMin,pMax,null,null,aId,notasCsv);
            d.dismiss();
        });
        btnClr.setOnClickListener(x -> { cargarPerfumes(); d.dismiss(); });

        d.show();
    }

    /* ===== SP filtros ===== */
    /* ===== SP filtros  (CORREGIDO) ===== */
    private void cargarPerfumesFiltrados(
            Integer marca, Integer genero, Integer tipo,
            Double  pMin,  Double pMax,
            Integer tMin,  Integer tMax,
            Integer aroma, String  notasCsv) {

        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                     CallableStatement cs = cn.prepareCall(
                             "{ call sp_ObtenerPerfumesFiltrados(?, ?, ?, ?, ?, ?, ?, ?, ?) }")) {

                    /* ---------- parámetros numéricos ---------- */
                    setIntOrNull(cs, 1, marca);
                    setIntOrNull(cs, 2, genero);
                    setIntOrNull(cs, 3, tipo);
                    setDecOrNull(cs, 4, pMin);
                    setDecOrNull(cs, 5, pMax);
                    setIntOrNull(cs, 6, tMin);
                    setIntOrNull(cs, 7, tMax);
                    setIntOrNull(cs, 8, aroma);

                    /* ---------- CSV de notas ---------- */
                    if (notasCsv == null || notasCsv.trim().isEmpty()) {
                        cs.setNull(9, java.sql.Types.VARCHAR);   // ← evita el –9
                    } else {
                        cs.setString(9, notasCsv);
                    }

                    /* ---------- ejecución ---------- */
                    ResultSet rs = cs.executeQuery();
                    List<Perfume> filtrados = new ArrayList<>();
                    while (rs.next()) {
                        filtrados.add(new Perfume(
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

                    runOnUiThread(() -> {
                        /* ① La lista global refleja el nuevo conjunto */
                        listaPerfumes.clear();
                        listaPerfumes.addAll(filtrados);

                        /* ② Refrescamos el adapter con la lista global */
                        adapter.updateData(listaPerfumes);

                        if (filtrados.isEmpty()) {
                            Toast.makeText(this,
                                    "No se encontraron perfumes con esos filtros",
                                    Toast.LENGTH_SHORT).show();
                        }

                        /* ③ Aplicamos cantidades sobre la lista filtrada */
                        cargarCantidadesCarrito();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Filtros error", e);
            }
        }).start();
    }


    /* ===== Utils ===== */
    private void setIntOrNull(CallableStatement cs,int idx,Integer val)throws Exception{
        if (val!=null) cs.setInt(idx,val); else cs.setNull(idx,java.sql.Types.INTEGER);}
    private void setDecOrNull(CallableStatement cs,int idx,Double val)throws Exception{
        if (val!=null) cs.setBigDecimal(idx,BigDecimal.valueOf(val)); else cs.setNull(idx,java.sql.Types.DECIMAL);}
    private Integer idOrNull(ComboItem c){ return c.getId()==0? null:c.getId(); }
    private Double numOrNull(EditText e){
        String s=e.getText().toString().trim(); return s.isEmpty()?null:Double.parseDouble(s);}

    private void cargarSpinner(String sp, Spinner target){
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try(Connection cn=DriverManager.getConnection(CONNECTION_URL);
                    CallableStatement cs=cn.prepareCall("{ call "+sp+" }");
                    ResultSet rs = cs.executeQuery()){

                    List<ComboItem> items = new ArrayList<>();
                    items.add(new ComboItem(0,"Todos"));
                    while(rs.next())
                        items.add(new ComboItem(rs.getInt("id"),rs.getString("nombre")));

                    runOnUiThread(()->{
                        ArrayAdapter<ComboItem> adp=new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item,items);
                        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        target.setAdapter(adp);
                    });
                }
            } catch (Exception e){
                Log.e(TAG,"Spinner "+sp,e);
            }
        }).start();
    }

    private void cargarNotasCheck(LinearLayout ll){
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try(Connection cn = DriverManager.getConnection(CONNECTION_URL);
                    CallableStatement cs = cn.prepareCall("{ call sp_GetNotas }");
                    ResultSet rs = cs.executeQuery()){

                    List<ComboItem> notas=new ArrayList<>();
                    while(rs.next()) notas.add(new ComboItem(rs.getInt("id"),rs.getString("nombre")));
                    runOnUiThread(()->{
                        ll.removeAllViews();
                        for(ComboItem n:notas){
                            CheckBox cb=new CheckBox(this);
                            cb.setText(n.toString());
                            cb.setTag(n.getId());
                            ll.addView(cb);
                        }
                    });
                }
            } catch (Exception e){
                Log.e(TAG,"Notas error",e);
            }
        }).start();
    }

    /* ===== Carrito (agregar / restar) ===== */
    private void agregarAlCarrito(Perfume p) {
        if (!isLogged || isGuest) {
            startActivity(new Intent(this, Login2.class));
            return;
        }

        new Thread(() -> {
            try (Connection cn  = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_AgregarCarrito(?, ?, ?) }")) {

                cs.setInt(1, p.getId());
                cs.setInt(2, prefs.getInt("clientId", -1));
                cs.setInt(3, 1);                // solo 1 unidad
                cs.execute();

                runOnUiThread(() -> {
                    /* 1️⃣ Ya NO volvemos a hacer p.setCantidad(+1) aquí */
                    cargarCantidadesCarrito();   // refrescamos todo desde la BD
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e(TAG, "Add carrito", e);
            }
        }).start();
    }

    private void restarDelCarrito(Perfume p) {
        if (!isLogged || isGuest) return;

        new Thread(() -> {
            try (Connection cn  = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_RestarCarrito(?, ?, ?) }")) {

                cs.setInt(1, p.getId());
                cs.setInt(2, prefs.getInt("clientId", -1));
                cs.registerOutParameter(3, java.sql.Types.INTEGER);
                cs.execute();

                if (cs.getInt(3) == 1) {
                    runOnUiThread(() -> {
                        /* 1️⃣ Quitamos la resta manual */
                        cargarCantidadesCarrito();
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Restar carrito", e);
            }
        }).start();
    }


    /* ===== Back & resume ===== */
    @Override
    protected void onResume() {
        super.onResume();

        // 1. Refrescar estado de sesión
        isLogged = prefs.getBoolean(KEY_LOGGED, false);
        isGuest  = prefs.getBoolean(KEY_GUEST , false);

        // 2. Actualizar encabezado
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText(isLogged && !isGuest
                ? "Bienvenido, " + prefs.getString(KEY_USER, "")
                : "Bienvenido, invitado");

        // 3. Visibilidad de ítems
        Menu m = navigationView.getMenu();
        boolean userReal = isLogged && !isGuest;
        m.findItem(R.id.nav_perfumes).setChecked(true);

        m.findItem(R.id.nav_profile)   .setVisible(userReal);
        m.findItem(R.id.nav_purchases) .setVisible(userReal);
        m.findItem(R.id.nav_logout)    .setVisible(userReal);
        if (m.findItem(R.id.nav_scan) != null) {
            m.findItem(R.id.nav_scan).setVisible(userReal);
        }
        m.findItem(R.id.nav_login)     .setVisible(!isLogged || isGuest);

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
