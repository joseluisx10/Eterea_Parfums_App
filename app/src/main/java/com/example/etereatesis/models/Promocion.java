package com.example.etereatesis.models;

public class Promocion {
    private int id;
    private String nombre;
    private String fechaInicio;
    private String fechaFin;
    private double descuento;
    private boolean activo;
    private String url;

    /* ---------- Constructor 7 parámetros ---------- */
    public Promocion(int id, String nombre, String fechaInicio, String fechaFin,
                     double descuento, boolean activo, String url) {
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.descuento = descuento;
        this.activo = activo;
        this.url = url;
    }

    /* ---------- Constructor 6 parámetros ---------- */
    public Promocion(int id, String nombre, String fechaInicio, String fechaFin,
                     double descuento, String url) {
        this(id, nombre, fechaInicio, fechaFin, descuento, true, url);  // activo=true por defecto
    }

    /* ---------- Getters ---------- */
    public int getId()             { return id; }
    public String getNombre()      { return nombre; }
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin()    { return fechaFin; }
    public double getDescuento()   { return descuento; }
    public boolean isActivo()      { return activo; }
    public String getUrl()         { return url; }
}
