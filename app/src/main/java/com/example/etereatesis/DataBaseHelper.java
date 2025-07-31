package com.example.etereatesis;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.etereatesis.models.Perfume;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataBaseHelper {

    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;


    public static Connection conectar() {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            return DriverManager.getConnection(CONNECTION_URL);
        } catch (ClassNotFoundException e) {
            Log.e("SQL_ERROR", "Driver JDBC no encontrado", e);
            return null;
        } catch (SQLException e) {
            Log.e("SQL_ERROR", "Error al conectar a SQL Server", e);
            return null;
        }
    }

    public static void actualizarCantidadCarrito(int clientId, int perfumeId, int nuevaCantidad, Callback callback) {
        new Thread(() -> {
            Connection conexion = null;
            CallableStatement stmt = null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conexion = DriverManager.getConnection(CONNECTION_URL);
                stmt = conexion.prepareCall("{ call sp_ActualizarCantidadCarrito(?, ?, ?) }");
                stmt.setInt(1, clientId);
                stmt.setInt(2, perfumeId);
                stmt.setInt(3, nuevaCantidad);
                stmt.execute();
                callback.onResult("Cantidad actualizada correctamente");
            } catch (Exception e) {
                callback.onResult("Error al actualizar cantidad: " + e.getMessage());
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Exception ex) {
                        // Ignorar error de cierre
                    }
                }
                if (conexion != null) {
                    try {
                        conexion.close();
                    } catch (Exception ex) {
                        // Ignorar error de cierre
                    }
                }
            }
        }).start();
    }


    public interface DiscountCallback {
        void onDiscountObtained(double discount);
    }

    public static void obtenerDescuentoParaPerfume(int perfumeId, DiscountCallback callback) {
        new Thread(() -> {
            Connection conexion = null;
            double discount = 0.0;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conexion = DriverManager.getConnection(CONNECTION_URL);
                CallableStatement stmt = conexion.prepareCall("{ call sp_ObtenerDescuentoParaPerfume(?, ?) }");
                stmt.setInt(1, perfumeId);
                stmt.registerOutParameter(2, java.sql.Types.DECIMAL);
                stmt.execute();
                BigDecimal dbDiscount = stmt.getBigDecimal(2);
                if (dbDiscount != null) {
                    discount = dbDiscount.doubleValue();
                }
                stmt.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (conexion != null) {
                    try {
                        conexion.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            double finalDiscount = discount;
            new Handler(Looper.getMainLooper()).post(() -> callback.onDiscountObtained(finalDiscount));
        }).start();
    }

    public static void eliminarProductoDeCarrito(int clientId, int productId, Callback callback) {
        new Thread(() -> {
            String resultado;
            Connection conexion = null;
            PreparedStatement stmt = null;
            try {
                conexion = conectar();
                if (conexion == null) {
                    resultado = "Error al conectar a la base de datos.";
                } else {
                    String sql = "DELETE FROM carrito WHERE cliente_id = ? AND perfume_id = ?";
                    stmt = conexion.prepareStatement(sql);
                    stmt.setInt(1, clientId);
                    stmt.setInt(2, productId);

                    int filasAfectadas = stmt.executeUpdate();
                    if (filasAfectadas > 0) {
                        resultado = "Producto eliminado correctamente ✅";
                    } else {
                        resultado = "No se encontró el producto para este cliente.";
                    }
                }
            } catch (SQLException e) {
                Log.e("SQL_ERROR", "Error al eliminar producto del carrito", e);
                resultado = "Error: " + e.getMessage();
            } finally {
                try {
                    if (stmt != null) stmt.close();
                    if (conexion != null) conexion.close();
                } catch (SQLException e) {
                    Log.e("SQL_ERROR", "Error al cerrar recursos", e);
                }
            }
            callback.onResult(resultado);
        }).start();
    }

    public static void agregarAlCarrito(int perfumeId, int cantidad, Callback callback) {
        new Thread(() -> {
            String resultado;
            Connection conexion = null;
            CallableStatement stmt = null;
            try {
                conexion = conectar();
                if (conexion == null) {
                    resultado = "Error al conectar a la base de datos.";
                } else {
                    stmt = conexion.prepareCall("{ call sp_AgregarCarrito(?, ?) }");
                    stmt.setInt(1, perfumeId);
                    stmt.setInt(2, cantidad);
                    stmt.execute();
                    resultado = "Añadido al carrito correctamente ✅";
                }
            } catch (SQLException e) {
                Log.e("SQL_ERROR", "Error al agregar al carrito", e);
                resultado = "Error: " + e.getMessage();
            } finally {
                try {
                    if (stmt != null) stmt.close();
                    if (conexion != null) conexion.close();
                } catch (SQLException e) {
                    Log.e("SQL_ERROR", "Error al cerrar recursos", e);
                }
            }
            callback.onResult(resultado);
        }).start();
    }

    public interface Callback {
        void onResult(String mensaje);
    }


    public static boolean insertarPerfume(String nombre, double precio, String imagenRuta) {
        String query = "INSERT INTO perfumes (nombre, precio, foto_url) VALUES (?, ?, ?)";
        Connection conexion = null;
        PreparedStatement stmt = null;
        try {
            conexion = conectar();
            if (conexion == null) {
                return false;
            }
            stmt = conexion.prepareStatement(query);
            stmt.setString(1, nombre);
            stmt.setDouble(2, precio);
            stmt.setString(3, imagenRuta);
            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            Log.e("SQL_ERROR", "Error al insertar perfume", e);
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conexion != null) conexion.close();
            } catch (SQLException e) {
                Log.e("SQL_ERROR", "Error al cerrar recursos", e);
            }
        }
    }

    public static List<Perfume> obtenerPerfumes() {
        List<Perfume> perfumes = new ArrayList<>();
        String query = "SELECT * FROM perfumes";
        Connection conexion = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conexion = conectar();
            if (conexion == null) {
                return perfumes;
            }
            stmt = conexion.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                perfumes.add(new Perfume(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getDouble("precio"),
                        rs.getString("foto_url")
                ));
            }
        } catch (SQLException e) {
            Log.e("SQL_ERROR", "Error al obtener perfumes", e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conexion != null) conexion.close();
            } catch (SQLException e) {
                Log.e("SQL_ERROR", "Error al cerrar recursos", e);
            }
        }
        return perfumes;
    }
}
