package com.example.etereatesis;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    // Campos personales
    private EditText etNombre, etApellido, etEmail, etPassword, etDNI,
            etFechaNacimiento, etTelefono;
    private Spinner spGenero, spCondicionIVA;

    // Spinners de dirección
    private Spinner spPais, spProvincia, spLocalidad, spCalle;

    // Otros campos de dirección
    private EditText etCodigoPostal, etNumeracionCalle, etPiso, etDepartamento;
    private Button btnRegister;

    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ──────── Inicializar views ────────
        etNombre          = findViewById(R.id.etNombre);
        etApellido        = findViewById(R.id.etApellido);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etDNI             = findViewById(R.id.etDNI);
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento);
        etTelefono        = findViewById(R.id.etTelefono);

        spCondicionIVA = findViewById(R.id.spCondicionIVA);
        spGenero       = findViewById(R.id.spGenero);
        spPais         = findViewById(R.id.spPais);
        spProvincia    = findViewById(R.id.spProvincia);
        spLocalidad    = findViewById(R.id.spLocalidad);
        spCalle        = findViewById(R.id.spCalle);

        etCodigoPostal    = findViewById(R.id.etCodigoPostal);
        etNumeracionCalle = findViewById(R.id.etNumeracionCalle);
        etPiso            = findViewById(R.id.etPiso);
        etDepartamento    = findViewById(R.id.etDepartamento);
        btnRegister       = findViewById(R.id.btnRegister);

        // ──────── Spinner Condición IVA ────────
        ArrayAdapter<CharSequence> ivaAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.condicion_iva_array,
                android.R.layout.simple_spinner_item
        );
        ivaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCondicionIVA.setAdapter(ivaAdapter);

        // ──────── Spinner Género ────────
        List<String> genderOptions = new ArrayList<>();
        genderOptions.add("Masculino");
        genderOptions.add("Femenino");
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGenero.setAdapter(genderAdapter);

        // ──────── Listas por defecto para cascada ────────
        final List<ComboItem> defaultProvincia = new ArrayList<>();
        defaultProvincia.add(new ComboItem(0, "Seleccione provincia"));
        final List<ComboItem> defaultLocalidad = new ArrayList<>();
        defaultLocalidad.add(new ComboItem(0, "Seleccione localidad"));
        final List<ComboItem> defaultCalle = new ArrayList<>();
        defaultCalle.add(new ComboItem(0, "Seleccione calle"));

        // ──────── Cascada País → Provincia → Localidad → Calle ────────
        loadSpinnerSinParametro("sp_get_paises", spPais, "Seleccione país");

        spPais.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                int paisId = ((ComboItem) spPais.getSelectedItem()).getId();
                if (paisId > 0) {
                    loadProvincias(paisId);
                } else {
                    setAdapter(spProvincia, defaultProvincia);
                    spProvincia.setSelection(0);
                }
                setAdapter(spLocalidad, defaultLocalidad);
                spLocalidad.setSelection(0);
                setAdapter(spCalle, defaultCalle);
                spCalle.setSelection(0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spProvincia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                int provId = ((ComboItem) spProvincia.getSelectedItem()).getId();
                if (provId > 0) {
                    loadLocalidades(provId);
                } else {
                    setAdapter(spLocalidad, defaultLocalidad);
                    spLocalidad.setSelection(0);
                }
                setAdapter(spCalle, defaultCalle);
                spCalle.setSelection(0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spLocalidad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                int locId = ((ComboItem) spLocalidad.getSelectedItem()).getId();
                if (locId > 0) {
                    loadCalles(locId);
                } else {
                    setAdapter(spCalle, defaultCalle);
                    spCalle.setSelection(0);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ──────── Inicializar Spinners de dirección en default ────────
        setAdapter(spProvincia, defaultProvincia);
        spProvincia.setSelection(0);
        setAdapter(spLocalidad, defaultLocalidad);
        spLocalidad.setSelection(0);
        setAdapter(spCalle, defaultCalle);
        spCalle.setSelection(0);

        // ──────── DatePicker para fecha de nacimiento ────────
        etFechaNacimiento.setFocusable(false);
        etFechaNacimiento.setClickable(true);
        etFechaNacimiento.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(
                    RegisterActivity.this,
                    android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth,
                    (DatePicker view, int y, int m, int d) ->
                            etFechaNacimiento.setText(String.format("%04d-%02d-%02d", y, m + 1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // ──────── Botón Registrar ────────
        btnRegister.setOnClickListener(v -> validarYRegistrar());
    }

    // ——————————— Métodos de carga de spinners ———————————
    private void loadSpinnerSinParametro(String spName, Spinner spinner, String defaultText) {
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call " + spName + " }")) {
                ResultSet rs = cs.executeQuery();
                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, defaultText));
                while (rs.next()) items.add(new ComboItem(rs.getInt("id"), rs.getString("nombre")));
                rs.close();
                runOnUiThread(() -> setAdapter(spinner, items));
            } catch (Exception e) {
                showError("Error al cargar datos: " + e.getMessage());
            }
        }).start();
    }

    private void loadProvincias(int paisId) {
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_get_provincias_por_pais(?) }")) {
                cs.setInt(1, paisId);
                ResultSet rs = cs.executeQuery();
                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, "Seleccione provincia"));
                while (rs.next()) items.add(new ComboItem(rs.getInt("id"), rs.getString("nombre")));
                rs.close();
                runOnUiThread(() -> setAdapter(spProvincia, items));
            } catch (Exception e) {
                showError("Error al cargar provincias: " + e.getMessage());
            }
        }).start();
    }

    private void loadLocalidades(int provId) {
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_get_localidades_por_provincia(?) }")) {
                cs.setInt(1, provId);
                ResultSet rs = cs.executeQuery();
                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, "Seleccione localidad"));
                while (rs.next()) items.add(new ComboItem(rs.getInt("id"), rs.getString("nombre")));
                rs.close();
                runOnUiThread(() -> setAdapter(spLocalidad, items));
            } catch (Exception e) {
                showError("Error al cargar localidades: " + e.getMessage());
            }
        }).start();
    }

    private void loadCalles(int locId) {
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_get_calles_por_localidad(?) }")) {
                cs.setInt(1, locId);
                ResultSet rs = cs.executeQuery();
                List<ComboItem> items = new ArrayList<>();
                items.add(new ComboItem(0, "Seleccione calle"));
                while (rs.next()) items.add(new ComboItem(rs.getInt("id"), rs.getString("nombre")));
                rs.close();
                runOnUiThread(() -> setAdapter(spCalle, items));
            } catch (Exception e) {
                showError("Error al cargar calles: " + e.getMessage());
            }
        }).start();
    }

    private void setAdapter(Spinner sp, List<ComboItem> items) {
        ArrayAdapter<ComboItem> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    // ——————————— Registro de usuario ———————————
    private void validarYRegistrar() {
        if (!validarCampos()) return;

        ComboItem pais  = (ComboItem) spPais.getSelectedItem();
        ComboItem prov  = (ComboItem) spProvincia.getSelectedItem();
        ComboItem loc   = (ComboItem) spLocalidad.getSelectedItem();
        ComboItem calle = (ComboItem) spCalle.getSelectedItem();

        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                 CallableStatement cs = cn.prepareCall("{ call sp_RegistrarCliente(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")) {

                cs.setString(1, etNombre.getText().toString().trim());
                cs.setString(2, etApellido.getText().toString().trim());
                cs.setString(3, etEmail.getText().toString().trim());
                cs.setString(4, hashPassword(etPassword.getText().toString().trim()));
                cs.setString(5, etDNI.getText().toString().trim());
                cs.setDate(6, java.sql.Date.valueOf(etFechaNacimiento.getText().toString().trim()));
                cs.setString(7, spGenero.getSelectedItem().toString());
                cs.setString(8, etTelefono.getText().toString().trim());
                cs.setString(9, spCondicionIVA.getSelectedItem().toString());
                cs.setInt(10, pais.getId());
                cs.setInt(11, prov.getId());
                cs.setInt(12, loc.getId());
                cs.setInt(13, calle.getId());
                cs.setString(14, etCodigoPostal.getText().toString().trim());
                cs.setString(15, etNumeracionCalle.getText().toString().trim());
                cs.setString(16, etPiso.getText().toString().trim().isEmpty() ? null : etPiso.getText().toString().trim());
                cs.setString(17, etDepartamento.getText().toString().trim().isEmpty() ? null : etDepartamento.getText().toString().trim());
                cs.registerOutParameter(18, java.sql.Types.INTEGER);

                cs.execute();
                int res = cs.getInt(18);

                runOnUiThread(() -> {
                    if (res == 1) {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, Login2.class));
                        finish();
                    } else {
                        Toast.makeText(this, "El email ya está registrado", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        }).start();
    }

    private boolean validarCampos() {
        if (etNombre.getText().toString().trim().isEmpty() ||
                etApellido.getText().toString().trim().isEmpty() ||
                etEmail.getText().toString().trim().isEmpty() ||
                etPassword.getText().toString().trim().isEmpty() ||
                etDNI.getText().toString().trim().isEmpty() ||
                etFechaNacimiento.getText().toString().trim().isEmpty() ||
                etTelefono.getText().toString().trim().isEmpty()) {
            showError("Todos los campos personales son obligatorios");
            return false;
        }
        if (!etEmail.getText().toString().contains("@")) {
            showError("Email inválido");
            return false;
        }
        if (etPassword.getText().toString().trim().length() < 6) {
            showError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        if (!validarSpinnersDireccion()) return false;
        if (etCodigoPostal.getText().toString().trim().isEmpty() ||
                etNumeracionCalle.getText().toString().trim().isEmpty()) {
            showError("Código Postal y Numeración Calle son obligatorios");
            return false;
        }
        return true;
    }

    private boolean validarSpinnersDireccion() {
        if (((ComboItem) spPais.getSelectedItem()).getId() == 0)  { showError("Seleccione país"); return false; }
        if (((ComboItem) spProvincia.getSelectedItem()).getId() == 0) { showError("Seleccione provincia"); return false; }
        if (((ComboItem) spLocalidad.getSelectedItem()).getId() == 0) { showError("Seleccione localidad"); return false; }
        if (((ComboItem) spCalle.getSelectedItem()).getId() == 0) { showError("Seleccione calle"); return false; }
        return true;
    }

    private void showError(String msg) {
        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show());
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

    // Clase auxiliar para los spinners
    public class ComboItem {
        private final int id;
        private final String name;
        public ComboItem(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        @Override public String toString() { return name; }
    }
}
