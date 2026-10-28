package com.studymatch.model;

import java.time.LocalDateTime;

public class E_Calificacion {

    private String id;
    private String idSesion;
    private String idEstudianteCalificador; // Para trazabilidad interna y control de duplicidad
    private int estrellas; // Escala de 1 a 5
    private String fechaCalificacion;
    private boolean anonimo;

    public E_Calificacion() {}

    public E_Calificacion(
        String id,
        String idSesion,
        String idEstudianteCalificador,
        int estrellas
    ) {
        this.id = id;
        this.idSesion = idSesion;
        this.idEstudianteCalificador = idEstudianteCalificador;
        this.estrellas = estrellas;
        this.fechaCalificacion = LocalDateTime.now().toString();
        this.anonimo = true; // Persistencia anónima
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(String idSesion) {
        this.idSesion = idSesion;
    }

    public String getIdEstudianteCalificador() {
        return idEstudianteCalificador;
    }

    public void setIdEstudianteCalificador(String idEstudianteCalificador) {
        this.idEstudianteCalificador = idEstudianteCalificador;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    public String getFechaCalificacion() {
        return fechaCalificacion;
    }

    public void setFechaCalificacion(String fechaCalificacion) {
        this.fechaCalificacion = fechaCalificacion;
    }

    public boolean isAnonimo() {
        return anonimo;
    }

    public void setAnonimo(boolean anonimo) {
        this.anonimo = anonimo;
    }
}
