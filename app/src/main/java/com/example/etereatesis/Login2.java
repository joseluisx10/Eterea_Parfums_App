package com.example.etereatesis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicReference;

public class Login2 extends AppCompatActivity {

    /* ---------- UI ---------- */
    private EditText etUsername, etPassword;
    private Button   btnLogin, btnGuest;
    private TextView tvCreateAccount, tvRecoverPassword;

    /* ---------- Prefs ---------- */
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME     = "LoginPrefs";
    private static final String KEY_USERNAME  = "username";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST  = "isGuest";

    /* ---------- BD ---------- */
    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    /* =================================================================== */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* 1️⃣  Salta al Main solo si hay usuario REAL */
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isLogged = sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
        boolean isGuest  = sharedPreferences.getBoolean(KEY_IS_GUEST , false);
        if (isLogged && !isGuest) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        /* 2️⃣  UI */
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return insets;
        });

        etUsername        = findViewById(R.id.etUsername);
        etPassword        = findViewById(R.id.etPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        btnGuest          = findViewById(R.id.btnGuest);
        tvCreateAccount   = findViewById(R.id.tvCreateAccount);
        tvRecoverPassword = findViewById(R.id.tvRecoverPassword);

        /* 3️⃣  Listeners */
        btnLogin.setOnClickListener(v -> validarUsuario());
        btnGuest.setOnClickListener(v -> entrarComoInvitado());
        tvCreateAccount.setOnClickListener(
                v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvRecoverPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /* ===================  LOGIN NORMAL =================== */
    private void validarUsuario() {
        String email    = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashed = SecurityUtils.hashPassword(password);
        AtomicReference<String> nombre = new AtomicReference<>("");

        new Thread(() -> {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                try (Connection cn = DriverManager.getConnection(CONNECTION_URL);
                     CallableStatement cs = cn.prepareCall("{ call sp_VerificarUsuario(?, ?, ?) }")) {

                    cs.setString(1, email);
                    cs.setString(2, hashed);
                    cs.registerOutParameter(3, java.sql.Types.INTEGER);

                    int clientId = -1;
                    if (cs.execute()) {
                        try (ResultSet rs = cs.getResultSet()) {
                            if (rs.next()) {
                                clientId = rs.getInt("id");
                                nombre.set(rs.getString("NombreUsuario"));
                            }
                        }
                    }

                    int ok = cs.getInt(3);
                    DETECTOR.clientId = clientId;

                    if (ok == 1) {
                        guardarSesionReal(
                                nombre.get().isEmpty() ? email : nombre.get(),
                                clientId);
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(this,
                                        "Usuario o contraseña incorrectos",
                                        Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error de conexión: " + ex.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void guardarSesionReal(String usuario, int clientId) {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putBoolean(KEY_IS_GUEST , false)
                .putString (KEY_USERNAME , usuario)
                .putInt    ("clientId"   , clientId)
                .apply();

        runOnUiThread(() -> {
            Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    /* =================  ENTRAR COMO INVITADO  ================= */
    private void entrarComoInvitado() {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)   // invitado ≠ login real
                .putBoolean(KEY_IS_GUEST , true)
                .putString (KEY_USERNAME , "Invitado")
                .apply();

        Toast.makeText(this, "Ingresaste como invitado", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /* ================  RECUPERAR CONTRASEÑA ================== */
    private void showForgotPasswordDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Recuperar Contraseña");
        EditText input = new EditText(this);
        input.setHint("Ingresa tu correo registrado");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(50, 40, 50, 40);
        b.setView(input);

        b.setPositiveButton("Enviar correo", (d, w) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Ingrese un correo válido", Toast.LENGTH_SHORT).show();
                return;
            }
            enviarCorreoRecuperacion(email);
        });
        b.setNegativeButton("Cancelar", (d,w)-> d.dismiss());
        b.show();
    }

    private void enviarCorreoRecuperacion(String email) {
        new Thread(() -> {
            try {
                /* Configura tus credenciales reales */
                String user = ""; // TODO
                String pass = ""; // TODO
                String subj = "Recuperar Contraseña";
                String body = "Para recuperar tu contraseña visita: http://tusitio.com/recuperar";

                new MailSender(user, pass).sendMail(subj, body, user, email);

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Correo enviado a " + email,
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, ResetPasswordActivity.class)
                            .putExtra("email", email));
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Error al enviar correo: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, ResetPasswordActivity.class)
                            .putExtra("email", email));
                });
            }
        }).start();
    }
}
