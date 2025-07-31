package com.example.etereatesis;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VerPerfil extends AppCompatActivity {

    /* ──────────── UI ──────────── */
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

    private EditText etUsuario, etClave, etNombre, etApellido, etDNI,
            etFechaNacimiento, etCelular, etEmail,
            etCodigoPostal, etNumeracionCalle,
            etPiso, etDepartamento, etComentarios;

    private Spinner spinnerCondicionIVA,
            spinnerPais, spinnerProvincia, spinnerLocalidad, spinnerCalle;

    private Button btnGuardar;

    /* ──────────── Otros ──────────── */
    private ArrayAdapter<CharSequence> condAdapter;
    private int clientId;
    private String originalPasswordHash = "";

    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    /* ══════════════════════════════════════════════ */
    /*                     onCreate                   */
    /* ══════════════════════════════════════════════ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_perfil);

        /* —— Toolbar + Drawer —— */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this::onNavItemSelected);

        /* —— Referencias —— */
        etUsuario = findViewById(R.id.etUsuario);
        etClave   = findViewById(R.id.etClave);
        etNombre  = findViewById(R.id.etNombre);
        etApellido= findViewById(R.id.etApellido);
        etDNI     = findViewById(R.id.etDNI);
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento);
        etCelular = findViewById(R.id.etCelular);
        etEmail   = findViewById(R.id.etEmail);
        etCodigoPostal    = findViewById(R.id.etCodigoPostal);
        etNumeracionCalle = findViewById(R.id.etNumeracionCalle);
        etPiso            = findViewById(R.id.etPiso);
        etDepartamento    = findViewById(R.id.etDepartamento);
        etComentarios     = findViewById(R.id.etComentarios);
        btnGuardar        = findViewById(R.id.btnGuardar);

        spinnerCondicionIVA = findViewById(R.id.spinnerCondicionIVA);
        spinnerPais      = findViewById(R.id.spinnerPais);
        spinnerProvincia = findViewById(R.id.spinnerProvincia);
        spinnerLocalidad = findViewById(R.id.spinnerLocalidad);
        spinnerCalle     = findViewById(R.id.spinnerCalle);

        /* —— Spinner Condición IVA —— */
        condAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.condicion_iva_array,
                android.R.layout.simple_spinner_item);
        condAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondicionIVA.setAdapter(condAdapter);

        /* —— Cargar spinners de dirección (con “Seleccione …”) —— */
        loadSpinnerData("SELECT id,nombre FROM pais",      spinnerPais,      "Seleccione país");
        loadSpinnerData("SELECT id,nombre FROM provincia", spinnerProvincia, "Seleccione provincia");
        loadSpinnerData("SELECT id,nombre FROM localidad", spinnerLocalidad, "Seleccione localidad");
        loadSpinnerData("SELECT id,nombre FROM calle",     spinnerCalle,     "Seleccione calle");

        /* —— DatePicker —— */
        etFechaNacimiento.setFocusable(false);
        etFechaNacimiento.setClickable(true);
        etFechaNacimiento.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(
                    VerPerfil.this,
                    (DatePicker view, int y, int m, int d) ->
                            etFechaNacimiento.setText(String.format(Locale.getDefault(),
                                    "%04d-%02d-%02d", y, m + 1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        /* —— Obtener clientId de sesión —— */
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        clientId = prefs.getInt("clientId", -1);
        if (clientId < 0) {
            Toast.makeText(this, "ID de cliente no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /* —— Traer datos del cliente —— */
        new ObtenerClienteTask().execute(clientId);

        /* —— Guardar cambios —— */
        btnGuardar.setOnClickListener(v -> {
            if (!validarSpinnersDireccion()) return;
            Cliente c = construirClienteDesdeFormulario();
            new EditarClienteTask().execute(c);
        });
    }

    /* ══════════════════════════════════════════════ */
    /*              Navegación Drawer                 */
    /* ══════════════════════════════════════════════ */
    private boolean onNavItemSelected(MenuItem item) {
        int id = item.getItemId();
        if      (id == R.id.nav_logout)    cerrarSesion();
        else if (id == R.id.nav_profile)   {/* ya aquí */}
        else if (id == R.id.nav_faq)       startActivity(new Intent(this, PreguntasFrecuentes.class));
        else if (id == R.id.nav_purchases) startActivity(new Intent(this, MisCompras.class));
        else if (id == R.id.nav_about)     startActivity(new Intent(this, SobreNosotros.class));
        else if (id == R.id.nav_promotions)startActivity(new Intent(this, Promociones.class));
        else if (id == R.id.nav_branches)  startActivity(new Intent(this, MapsActivity.class));
        else if (id == R.id.nav_perfumes)  startActivity(new Intent(this, MainActivity.class));
        else if (id == R.id.nav_scan)      startActivity(new Intent(this, ScanUPCActivity.class));
        else if (id == R.id.nav_login)     startActivity(new Intent(this, Login2.class));

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /* ══════════════════════════════════════════════ */
    /*         Helpers: Spinners y validaciones       */
    /* ══════════════════════════════════════════════ */
    private void loadSpinnerData(String query, Spinner spinner, String defaultText) {
        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection cn = DriverManager.getConnection(CONNECTION_URL);
                Statement st  = cn.createStatement();
                ResultSet rs  = st.executeQuery(query);

                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, defaultText));         // opción por defecto
                while (rs.next()) items.add(
                        new ComboItem(rs.getInt("id"), rs.getString("nombre")));

                rs.close(); st.close(); cn.close();

                runOnUiThread(() -> {
                    ArrayAdapter<ComboItem> adp = new ArrayAdapter<>(
                            VerPerfil.this,
                            android.R.layout.simple_spinner_item,
                            items);
                    adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adp);
                    spinner.setSelection(0);                       // forzamos “Seleccione …”
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(VerPerfil.this,
                                "Error al cargar datos: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean validarSpinnersDireccion() {
        if (((ComboItem) spinnerPais.getSelectedItem()).getId() == 0) {
            Toast.makeText(this, "Debe seleccionar un país", Toast.LENGTH_SHORT).show(); return false;
        }
        if (((ComboItem) spinnerProvincia.getSelectedItem()).getId() == 0) {
            Toast.makeText(this, "Debe seleccionar una provincia", Toast.LENGTH_SHORT).show(); return false;
        }
        if (((ComboItem) spinnerLocalidad.getSelectedItem()).getId() == 0) {
            Toast.makeText(this, "Debe seleccionar una localidad", Toast.LENGTH_SHORT).show(); return false;
        }
        if (((ComboItem) spinnerCalle.getSelectedItem()).getId() == 0) {
            Toast.makeText(this, "Debe seleccionar una calle", Toast.LENGTH_SHORT).show(); return false;
        }
        return true;
    }

    /* ══════════════════════════════════════════════ */
    /*           Construir objeto Cliente             */
    /* ══════════════════════════════════════════════ */
    private Cliente construirClienteDesdeFormulario() {
        Cliente c = new Cliente();
        c.setId(clientId);
        c.setUsuario(etUsuario.getText().toString().trim());
        String pw = etClave.getText().toString().trim();
        c.setClave(pw.isEmpty() ? originalPasswordHash : hashPassword(pw));
        c.setNombre(etNombre.getText().toString().trim());
        c.setApellido(etApellido.getText().toString().trim());
        c.setDni(etDNI.getText().toString().trim());
        c.setCondicionFrenteAlIva(spinnerCondicionIVA.getSelectedItem().toString());

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            c.setFechaNacimiento(df.parse(etFechaNacimiento.getText().toString()));
        } catch (ParseException ignored) {}

        c.setCelular(etCelular.getText().toString().trim());
        c.setEMail(etEmail.getText().toString().trim());
        c.setPaisId(((ComboItem) spinnerPais.getSelectedItem()).getId());
        c.setProvinciaId(((ComboItem) spinnerProvincia.getSelectedItem()).getId());
        c.setLocalidadId(((ComboItem) spinnerLocalidad.getSelectedItem()).getId());
        c.setCalleId(((ComboItem) spinnerCalle.getSelectedItem()).getId());
        c.setCodigoPostal(etCodigoPostal.getText().toString().trim());
        c.setNumeracionCalle(etNumeracionCalle.getText().toString().trim());
        c.setPiso(etPiso.getText().toString().trim());
        c.setDepartamento(etDepartamento.getText().toString().trim());
        c.setComentariosDomicilio(etComentarios.getText().toString().trim());
        c.setActivo(true);
        c.setRol("cliente");
        return c;
    }

    /* ══════════════════════════════════════════════ */
    /*                 Utilidades                     */
    /* ══════════════════════════════════════════════ */
    private void cerrarSesion() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, Login2.class));
        finish();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar la contraseña", e);
        }
    }

    // AsyncTask para obtener cliente
    private class ObtenerClienteTask extends AsyncTask<Integer, Void, Cliente> {
        @Override
        protected Cliente doInBackground(Integer... params) {
            Cliente cliente = null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection con = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement stmt = con.prepareCall("{call sp_ObtenerCliente(?)}");
                stmt.setInt(1, params[0]);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    cliente = new Cliente();
                    cliente.setId(rs.getInt("id"));
                    cliente.setUsuario(rs.getString("usuario"));
                    cliente.setClave(rs.getString("clave"));
                    cliente.setNombre(rs.getString("nombre"));
                    cliente.setApellido(rs.getString("apellido"));
                    cliente.setDni(rs.getString("dni"));
                    cliente.setCondicionFrenteAlIva(rs.getString("condicion_frente_al_iva"));
                    cliente.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
                    cliente.setCelular(rs.getString("celular"));
                    cliente.setEMail(rs.getString("e_mail"));
                    cliente.setPaisId(rs.getInt("pais_id"));
                    cliente.setProvinciaId(rs.getInt("provincia_id"));
                    cliente.setLocalidadId(rs.getInt("localidad_id"));
                    cliente.setCodigoPostal(rs.getString("codigo_postal"));
                    cliente.setCalleId(rs.getInt("calle_id"));
                    cliente.setNumeracionCalle(rs.getString("numeracion_calle"));
                    cliente.setPiso(rs.getString("piso"));
                    cliente.setDepartamento(rs.getString("departamento"));
                    cliente.setComentariosDomicilio(rs.getString("comentarios_domicilio"));
                    cliente.setActivo(rs.getBoolean("activo"));
                    cliente.setRol(rs.getString("rol"));
                }
                rs.close();
                stmt.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cliente;
        }

        @Override
        protected void onPostExecute(Cliente cliente) {
            if (cliente != null) {
                etUsuario.setText(cliente.getUsuario());
                etClave.setText("");
                etClave.setHint("********");
                originalPasswordHash = cliente.getClave();
                etNombre.setText(cliente.getNombre());
                etApellido.setText(cliente.getApellido());
                etDNI.setText(cliente.getDni());
                // Spinner AFIP
                String cond = cliente.getCondicionFrenteAlIva();
                for (int i = 0; i < condAdapter.getCount(); i++) {
                    if (condAdapter.getItem(i).equals(cond)) {
                        spinnerCondicionIVA.setSelection(i);
                        break;
                    }
                }
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etFechaNacimiento.setText(
                        cliente.getFechaNacimiento() != null
                                ? df.format(cliente.getFechaNacimiento())
                                : ""
                );
                etCelular.setText(cliente.getCelular());
                etEmail.setText(cliente.getEMail());
                etCodigoPostal.setText(cliente.getCodigoPostal());
                etNumeracionCalle.setText(cliente.getNumeracionCalle());
                etPiso.setText(cliente.getPiso());
                etDepartamento.setText(cliente.getDepartamento());
                etComentarios.setText(cliente.getComentariosDomicilio());

                // Delay selection for address spinners
                setSpinnerSelectionWithDelay(spinnerPais, cliente.getPaisId(), 500);
                setSpinnerSelectionWithDelay(spinnerProvincia, cliente.getProvinciaId(), 500);
                setSpinnerSelectionWithDelay(spinnerLocalidad, cliente.getLocalidadId(), 500);
                setSpinnerSelectionWithDelay(spinnerCalle, cliente.getCalleId(), 500);
            } else {
                Toast.makeText(VerPerfil.this, "Error al cargar los datos del cliente.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // AsyncTask para editar cliente
    private class EditarClienteTask extends AsyncTask<Cliente, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Cliente... params) {
            Cliente c = params[0];
            boolean success = false;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection con = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement stmt = con.prepareCall("{call sp_EditarCliente(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
                stmt.setInt(1, c.getId());
                stmt.setString(2, c.getUsuario());
                stmt.setString(3, c.getClave());
                stmt.setString(4, c.getNombre());
                stmt.setString(5, c.getApellido());
                stmt.setString(6, c.getDni());
                stmt.setString(7, c.getCondicionFrenteAlIva());
                if (c.getFechaNacimiento() != null) {
                    stmt.setDate(8, new java.sql.Date(c.getFechaNacimiento().getTime()));
                } else {
                    stmt.setDate(8, null);
                }
                stmt.setString(9, c.getCelular());
                stmt.setString(10, c.getEMail());
                stmt.setInt(11, c.getPaisId());
                stmt.setInt(12, c.getProvinciaId());
                stmt.setInt(13, c.getLocalidadId());
                stmt.setString(14, c.getCodigoPostal());
                stmt.setInt(15, c.getCalleId());
                stmt.setString(16, c.getNumeracionCalle());
                stmt.setString(17, c.getPiso());
                stmt.setString(18, c.getDepartamento());
                stmt.setString(19, c.getComentariosDomicilio());
                stmt.setBoolean(20, c.isActivo());
                stmt.setString(21, c.getRol());

                success = stmt.executeUpdate() > 0;
                stmt.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Toast.makeText(
                    VerPerfil.this,
                    success
                            ? "Datos actualizados correctamente."
                            : "Error al actualizar los datos.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Helper para seleccionar spinner por ID con delay
    private void setSpinnerSelectionWithDelay(final Spinner spinner, final int id, long delay) {
        spinner.postDelayed(() -> {
            ArrayAdapter<ComboItem> adapter = (ArrayAdapter<ComboItem>) spinner.getAdapter();
            if (adapter != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).getId() == id) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        }, delay);
    }

    // Modelo de datos para el cliente
    public class Cliente {
        private int id;
        private String usuario;
        private String clave;
        private String nombre;
        private String apellido;
        private String dni;
        private String condicionFrenteAlIva;
        private Date fechaNacimiento;
        private String celular;
        private String eMail;
        private int paisId;
        private int provinciaId;
        private int localidadId;
        private String codigoPostal;
        private int calleId;
        private String numeracionCalle;
        private String piso;
        private String departamento;
        private String comentariosDomicilio;
        private boolean activo;
        private String rol;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getClave() { return clave; }
        public void setClave(String clave) { this.clave = clave; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getApellido() { return apellido; }
        public void setApellido(String apellido) { this.apellido = apellido; }
        public String getDni() { return dni; }
        public void setDni(String dni) { this.dni = dni; }
        public String getCondicionFrenteAlIva() { return condicionFrenteAlIva; }
        public void setCondicionFrenteAlIva(String condicionFrenteAlIva) { this.condicionFrenteAlIva = condicionFrenteAlIva; }
        public Date getFechaNacimiento() { return fechaNacimiento; }
        public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
        public String getCelular() { return celular; }
        public void setCelular(String celular) { this.celular = celular; }
        public String getEMail() { return eMail; }
        public void setEMail(String eMail) { this.eMail = eMail; }
        public int getPaisId() { return paisId; }
        public void setPaisId(int paisId) { this.paisId = paisId; }
        public int getProvinciaId() { return provinciaId; }
        public void setProvinciaId(int provinciaId) { this.provinciaId = provinciaId; }
        public int getLocalidadId() { return localidadId; }
        public void setLocalidadId(int localidadId) { this.localidadId = localidadId; }
        public String getCodigoPostal() { return codigoPostal; }
        public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
        public int getCalleId() { return calleId; }
        public void setCalleId(int calleId) { this.calleId = calleId; }
        public String getNumeracionCalle() { return numeracionCalle; }
        public void setNumeracionCalle(String numeracionCalle) { this.numeracionCalle = numeracionCalle; }
        public String getPiso() { return piso; }
        public void setPiso(String piso) { this.piso = piso; }
        public String getDepartamento() { return departamento; }
        public void setDepartamento(String departamento) { this.departamento = departamento; }
        public String getComentariosDomicilio() { return comentariosDomicilio; }
        public void setComentariosDomicilio(String comentariosDomicilio) { this.comentariosDomicilio = comentariosDomicilio; }
        public boolean isActivo() { return activo; }
        public void setActivo(boolean activo) { this.activo = activo; }
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
    }

    // Clase para representar cada elemento de los spinners
    public class ComboItem {
        private int id;
        private String name;
        public ComboItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        @Override
        public String toString() { return name; }
    }
}
