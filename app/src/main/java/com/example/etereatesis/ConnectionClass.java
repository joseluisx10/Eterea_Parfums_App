package com.example.etereatesis;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.security.auth.callback.Callback;

public class ConnectionClass {
    // Datos de conexión
    private static final String IP = "192.168.56.1";
    private static final String PORT = "1433";
    private static final String DB = "eterea";
    private static final String USER = "JLUIS\\MSSQLSERVER2025";
    private static final String PASSWORD = "12345";

    // Método para obtener la conexión con SQL Server
    public Connection CONN() {
        Connection conn = null;
        String conUrl = "jdbc:jtds:sqlserver://" + IP + ":" + PORT + "/" + DB +
                ";user=" + USER + ";password=" + PASSWORD + ";";

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            conn = DriverManager.getConnection(conUrl);
            Log.d("SUCCESS", "Conexión exitosa a SQL Server");
        } catch (SQLException e) {
            Log.e("ERROR1", "SQL Exception: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("ERROR2", "Driver no encontrado: " + e.getMessage());
        } catch (Exception e) {
            Log.e("ERROR3", "Excepción general: " + e.getMessage());
        }

        return conn;
    }

    public void pingDatabase(Callback callback) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            String resultado;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);

                if (conexion != null) {
                    resultado = "Conexión exitosa ✅";
                    conexion.close(); // Cerrar conexión después del ping
                } else {
                    resultado = "No se pudo conectar ❌";
                }
            } catch (ClassNotFoundException e) {
                resultado = "Error: Driver no encontrado";
            } catch (SQLException e) {
                resultado = "Error SQL: " + e.getMessage();
            } catch (Exception e) {
                resultado = "Error desconocido: " + e.getMessage();
            }

        });
    }

}
