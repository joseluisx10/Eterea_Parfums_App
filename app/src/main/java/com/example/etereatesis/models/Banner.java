// Banner.java
package com.example.etereatesis.models;

public class Banner {

    /* --- Atributos principales --- */
    private final int    idPromo;          // id fila perfumes_en_promo (si lo usas)
    private final String imagenRuta;       // URL de la imagen a mostrar
    private final int    perfumeId;        // id del perfume destino

    /* --- Datos del perfume (para DetalleProducto) --- */
    private final String nombrePerfume;
    private final double precioPromo;      // precio ya con descuento
    private final String descripcion;
    private final int    presentacionMl;
    private final String nombreMarca;

    public Banner(int idPromo,
                  int perfumeId,
                  String imagenRuta,
                  String nombrePerfume,
                  double precioPromo,
                  String descripcion,
                  int presentacionMl,
                  String nombreMarca) {

        this.idPromo        = idPromo;
        this.perfumeId      = perfumeId;
        this.imagenRuta     = imagenRuta;
        this.nombrePerfume  = nombrePerfume;
        this.precioPromo    = precioPromo;
        this.descripcion    = descripcion;
        this.presentacionMl = presentacionMl;
        this.nombreMarca    = nombreMarca;
    }

    /* --- Getters --- */
    public int    getIdPromo()        { return idPromo; }
    public int    getPerfumeId()      { return perfumeId; }
    public String getImagenRuta()     { return imagenRuta; }
    public String getNombrePerfume()  { return nombrePerfume; }
    public double getPrecioPromo()    { return precioPromo; }
    public String getDescripcion()    { return descripcion; }
    public int    getPresentacionMl() { return presentacionMl; }
    public String getNombreMarca()    { return nombreMarca; }
}
