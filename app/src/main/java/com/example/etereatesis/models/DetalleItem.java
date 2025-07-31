package com.example.etereatesis.models;

import java.io.Serializable;

public class DetalleItem implements Serializable {

    private String descripcion;      // ← nuevo
    private int cantidad;
    private double precioUnitario;
    private double subtotalItem;
    private int promocionId;

    public DetalleItem(String descripcion,
                       int cantidad,
                       double precioUnitario,
                       double subtotalItem,
                       int promocionId) {
        this.descripcion    = descripcion;
        this.cantidad       = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotalItem   = subtotalItem;
        this.promocionId    = promocionId;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotalItem() {
        return subtotalItem;
    }
    public void setSubtotalItem(double subtotalItem) {
        this.subtotalItem = subtotalItem;
    }

    public int getPromocionId() {
        return promocionId;
    }
    public void setPromocionId(int promocionId) {
        this.promocionId = promocionId;
    }
}
