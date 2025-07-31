package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class PreguntasFrecuentes extends AppCompatActivity {

    /* ======= PREFS ======= */
    private static final String PREF_NAME     = "LoginPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST  = "isGuest";
    private static final String KEY_USERNAME  = "username";

    /* ======= UI ======= */
    private DrawerLayout          drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView        navigationView;
    private RecyclerView          recyclerViewFaq;
    private FAQAdapter            faqAdapter;

    /* ======= DATA ======= */
    private final List<FAQ> faqList = new ArrayList<>();
    private SharedPreferences prefs;
    private boolean isLoggedIn, isGuest;

    /* ------------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preguntas_frecuentes);

        /* ---------- Sesión ---------- */
        prefs       = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLoggedIn  = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest     = prefs.getBoolean(KEY_IS_GUEST , false);
        String user = prefs.getString(KEY_USERNAME, "");

        /* ---------- Toolbar & Drawer ---------- */
        Toolbar toolbar = findViewById(R.id.toolbarPreguntasFrecuentes);
        setSupportActionBar(toolbar);

        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle   = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        /* Header */
        TextView tvUser = navigationView.getHeaderView(0)
                .findViewById(R.id.tv_user_name);
        tvUser.setText(isLoggedIn && !isGuest
                ? String.format("Bienvenido, %s", user)
                : "Bienvenido, invitado");
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        /* Visibilidad de ítems */
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile)   .setVisible(isLoggedIn && !isGuest);
        menu.findItem(R.id.nav_purchases) .setVisible(isLoggedIn && !isGuest);
        menu.findItem(R.id.nav_logout)    .setVisible(isLoggedIn && !isGuest);
        menu.findItem(R.id.nav_login)     .setVisible(!isLoggedIn);

        navigationView.setCheckedItem(R.id.nav_faq);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_home)        { startActivity(new Intent(this, MainActivity.class)); finish(); }
            else if (id == R.id.nav_logout)   { prefs.edit().clear().apply(); startActivity(new Intent(this, Login2.class)); finish(); }
            else if (id == R.id.nav_login)       startActivity(new Intent(this, Login2.class));
            else if (id == R.id.nav_profile)     startActivity(new Intent(this, VerPerfil.class));
            else if (id == R.id.nav_about)       startActivity(new Intent(this, SobreNosotros.class));
            else if (id == R.id.nav_promotions)  startActivity(new Intent(this, Promociones.class));
            else if (id == R.id.nav_branches)    startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_perfumes)   { startActivity(new Intent(this, Perfumes.class)); finish(); }
            else if (id == R.id.nav_scan)        startActivity(new Intent(this, ScanUPCActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        /* Edge-to-edge */
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v,insets)->{
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        /* RecyclerView */
        recyclerViewFaq = findViewById(R.id.recyclerViewFaq);
        recyclerViewFaq.setLayoutManager(new LinearLayoutManager(this));
        cargarPreguntasDemo();
        faqAdapter = new FAQAdapter(faqList);
        recyclerViewFaq.setAdapter(faqAdapter);
    }

    /* ------------------------------------------------------- */
    @Override
    protected void onResume() {
        super.onResume();
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        isGuest    = prefs.getBoolean(KEY_IS_GUEST , false);

        Menu m = navigationView.getMenu();
        m.findItem(R.id.nav_profile)   .setVisible(isLoggedIn && !isGuest);
        m.findItem(R.id.nav_purchases) .setVisible(isLoggedIn && !isGuest);
        m.findItem(R.id.nav_logout)    .setVisible(isLoggedIn && !isGuest);
        m.findItem(R.id.nav_login)     .setVisible(!isLoggedIn);
    }

    /* ------------------------------------------------------- */
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

    /* ======= Datos de ejemplo para FAQ ======= */
    private void cargarPreguntasDemo() {
        faqList.clear();
        faqList.add(new FAQ("¿Cómo puedo registrarme?",
                "Pulsa el botón «Crear cuenta» y completa el formulario."));
        faqList.add(new FAQ("Olvidé mi contraseña, ¿qué hago?",
                "Usa la opción «Restablecer contraseña» y sigue los pasos."));
        faqList.add(new FAQ("¿Cómo actualizo mis datos?",
                "Ve a tu perfil y toca «Editar datos personales»."));
        faqList.add(new FAQ("¿Qué métodos de pago aceptan?",
                "Tarjetas de crédito/débito, PayPal y transferencias."));
        faqList.add(new FAQ("¿Cómo contacto al soporte?",
                "Escríbenos al correo soporte@example.com o llama al 800-123-456."));
    }

    /* ======= Modelo & Adapter ======= */
    public static class FAQ {
        private final String q,a;
        public FAQ(String q,String a){this.q=q;this.a=a;}
        public String getQuestion(){return q;}
        public String getAnswer(){return a;}
    }

    public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.ViewHolder>{
        private final List<FAQ> data;
        public FAQAdapter(List<FAQ> d){data=d;}
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup p,int v){
            View vq=LayoutInflater.from(p.getContext()).inflate(R.layout.item_faq,p,false);
            return new ViewHolder(vq);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h,int i){
            FAQ f=data.get(i); h.q.setText(f.getQuestion()); h.a.setText(f.getAnswer());
        }
        @Override public int getItemCount(){return data.size();}
        class ViewHolder extends RecyclerView.ViewHolder{
            TextView q,a; ViewHolder(View v){super(v); q=v.findViewById(R.id.tvQuestion); a=v.findViewById(R.id.tvAnswer);}
        }
    }
}
