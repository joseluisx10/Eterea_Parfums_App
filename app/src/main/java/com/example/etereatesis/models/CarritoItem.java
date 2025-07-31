package com.example.etereatesis.models;

import java.io.Serializable;

public class CarritoItem implements Serializable {
    private int id;
    private String nombre;
    private double precio;
    private String fotoUrl;
    private int cantidad;
    private double descuento; // porcentaje o valor, según cómo lo manejes

    public CarritoItem(int id, String nombre, double precio, String fotoUrl, int cantidad) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.fotoUrl = fotoUrl;
        this.cantidad = cantidad;
        this.descuento = 0.0;
    }

    // Getters y setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public double getPrecio() { return precio; }
    public String getFotoUrl() { return fotoUrl; }
    public int getCantidad() { return cantidad; }
    public double getDescuento() { return descuento; }

    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    // Calcula el precio con descuento aplicado (supongamos que 'descuento' es en porcentaje)
    public double getPrecioConDescuento() {
        double precioFinal = precio - (precio * (descuento / 100.0));
        return Math.max(precioFinal, 0.0);
    }
}
