package com.example.etereatesis.models;

import java.util.Date;

// Modelo de datos para el cliente
public class Cliente {
    private int id;
    private String usuario;
    private String clave;
    private String nombre;
    private String apellido;
    private String dni;
    private String condicionFrenteAlIva;
    private Date fechaNacimiento;
    private String celular;
    private String eMail;
    private int paisId;
    private int provinciaId;
    private int localidadId;
    private String codigoPostal;
    private int calleId;
    private String numeracionCalle;
    private String piso;
    private String departamento;
    private String comentariosDomicilio;
    private boolean activo;
    private String rol;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getCondicionFrenteAlIva() { return condicionFrenteAlIva; }
    public void setCondicionFrenteAlIva(String condicionFrenteAlIva) { this.condicionFrenteAlIva = condicionFrenteAlIva; }
    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getEMail() { return eMail; }
    public void setEMail(String eMail) { this.eMail = eMail; }
    public int getPaisId() { return paisId; }
    public void setPaisId(int paisId) { this.paisId = paisId; }
    public int getProvinciaId() { return provinciaId; }
    public void setProvinciaId(int provinciaId) { this.provinciaId = provinciaId; }
    public int getLocalidadId() { return localidadId; }
    public void setLocalidadId(int localidadId) { this.localidadId = localidadId; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public int getCalleId() { return calleId; }
    public void setCalleId(int calleId) { this.calleId = calleId; }
    public String getNumeracionCalle() { return numeracionCalle; }
    public void setNumeracionCalle(String numeracionCalle) { this.numeracionCalle = numeracionCalle; }
    public String getPiso() { return piso; }
    public void setPiso(String piso) { this.piso = piso; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getComentariosDomicilio() { return comentariosDomicilio; }
    public void setComentariosDomicilio(String comentariosDomicilio) { this.comentariosDomicilio = comentariosDomicilio; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}

