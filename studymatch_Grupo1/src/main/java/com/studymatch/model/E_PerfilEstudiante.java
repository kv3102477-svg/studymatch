package com.studymatch.model;

public class E_PerfilEstudiante {

    private String id;
    private String correo;
    private String contrasena;
    private String nombre;
    private String carrera;
    private String ciclo;
    private String bio;
    private boolean pushEnabled = true;
    private int penalizaciones = 0;
    private String tipoSuscripcion = "GRATUITO";

    public E_PerfilEstudiante() {}

    public E_PerfilEstudiante(
        String id,
        String correo,
        String contrasena,
        String nombre,
        String carrera,
        String ciclo,
        String bio
    ) {
        this.id = id;
        this.correo = correo;
        this.contrasena = contrasena;
        this.nombre = nombre;
        this.carrera = carrera;
        this.ciclo = ciclo;
        this.bio = bio;
        this.tipoSuscripcion = "GRATUITO";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public String getCiclo() {
        return ciclo;
    }

    public void setCiclo(String ciclo) {
        this.ciclo = ciclo;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public int getPenalizaciones() {
        return penalizaciones;
    }

    public void setPenalizaciones(int penalizaciones) {
        this.penalizaciones = penalizaciones;
    }

    public String getTipoSuscripcion() {
        return tipoSuscripcion;
    }

    public void setTipoSuscripcion(String tipoSuscripcion) {
        this.tipoSuscripcion = tipoSuscripcion;
    }

    // Métodos representativos del diagrama de secuencias RF-0034
    public void actualizarTipoSuscripcion(String nuevoTipo) {
        this.tipoSuscripcion = nuevoTipo;
    }

    public void actualizarPermisos(String nuevoTipo) {
        this.tipoSuscripcion = nuevoTipo;
    }
}
