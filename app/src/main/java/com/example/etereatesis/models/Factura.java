package com.example.etereatesis.models;

import java.io.Serializable;
import java.util.Date;

public class Factura implements Serializable {
    private int numFactura;
    private Date fecha;
    private double precioTotal;
    private double descuento;
    private String formaPago;
    private int totalItems; // Por ejemplo, la suma de cantidades de detalle

    public Factura(int numFactura, Date fecha, double precioTotal, double descuento, String formaPago, int totalItems) {
        this.numFactura = numFactura;
        this.fecha = fecha;
        this.precioTotal = precioTotal;
        this.descuento = descuento;
        this.formaPago = formaPago;
        this.totalItems = totalItems;
    }

    public int getNumFactura() { return numFactura; }
    public Date getFecha() { return fecha; }
    public double getPrecioTotal() { return precioTotal; }
    public double getDescuento() { return descuento; }
    public String getFormaPago() { return formaPago; }
    public int getTotalItems() { return totalItems; }
}
