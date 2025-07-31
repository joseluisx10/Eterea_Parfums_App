package com.example.etereatesis.models;

public class Perfume {
    private int id;
    private String nombre;
    private double precio;
    private String imagenRuta;
    private int cantidad;        // Para el carrito, arranca en 0
    private String varCodeUPC;   // UPC
    private String descripcion;  // Descripción del perfume
    private int presentacionML;  // Tamaño en ml
    private String nombreMarca;  // Marca del perfume

    // Nuevos campos para promociones
    private boolean enPromocion;
    private double precioOriginal;
    private double precioPromocion;

    // Constructor mínimo (cantidad=0)
    public Perfume(int id, String nombre, double precio, String imagenRuta) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.imagenRuta = imagenRuta;
        this.cantidad = 1;
    }

    // Constructor con UPC
    public Perfume(int id, String nombre, double precio, String imagenRuta, String varCodeUPC) {
        this(id, nombre, precio, imagenRuta);
        this.varCodeUPC = varCodeUPC;
    }

    // Constructor completo con UPC, descripción y presentación_ml
    public Perfume(int id, String nombre, double precio, String imagenRuta,
                   String varCodeUPC, String descripcion, int presentacionML) {
        this(id, nombre, precio, imagenRuta, varCodeUPC);
        this.descripcion = descripcion;
        this.presentacionML = presentacionML;
    }

    // Constructor completo incluyendo marca
    public Perfume(int id, String nombre, double precio, String imagenRuta,
                   String varCodeUPC, String descripcion, int presentacionML, String nombreMarca) {
        this(id, nombre, precio, imagenRuta, varCodeUPC, descripcion, presentacionML);
        this.nombreMarca = nombreMarca;
    }

    // Si quisieras inicializar con una cantidad concreta:
    public Perfume(int id, String nombre, double precio, String imagenRuta,
                   String varCodeUPC, String descripcion, int presentacionML,
                   String nombreMarca, int cantidadInicial) {
        this(id, nombre, precio, imagenRuta, varCodeUPC, descripcion, presentacionML, nombreMarca);
        this.cantidad = cantidadInicial;
    }

    // Nuevo constructor para promociones: incluye precioOriginal y precioPromocion
    public Perfume(int id,
                   String nombre,
                   String imagenRuta,
                   String varCodeUPC,
                   String descripcion,
                   int presentacionML,
                   String nombreMarca,
                   double precioOriginal,
                   double precioPromocion) {
        // inicializa con precioPromocion como precio base
        this(id, nombre, precioPromocion, imagenRuta, varCodeUPC, descripcion, presentacionML, nombreMarca);
        this.enPromocion = true;
        this.precioOriginal = precioOriginal;
        this.precioPromocion = precioPromocion;
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public double getPrecio() { return precio; }
    public String getImagenRuta() { return imagenRuta; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getVarCodeUPC() { return varCodeUPC; }
    public void setVarCodeUPC(String varCodeUPC) { this.varCodeUPC = varCodeUPC; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getPresentacionML() { return presentacionML; }
    public void setPresentacionML(int presentacionML) { this.presentacionML = presentacionML; }
    public String getNombreMarca() { return nombreMarca; }
    public void setNombreMarca(String nombreMarca) { this.nombreMarca = nombreMarca; }

    // Getters y Setters para promoción
    public boolean isEnPromocion() { return enPromocion; }
    public void setEnPromocion(boolean enPromocion) { this.enPromocion = enPromocion; }
    public double getPrecioOriginal() { return precioOriginal; }
    public void setPrecioOriginal(double precioOriginal) { this.precioOriginal = precioOriginal; }
    public double getPrecioPromocion() { return precioPromocion; }
    public void setPrecioPromocion(double precioPromocion) { this.precioPromocion = precioPromocion; }
}
