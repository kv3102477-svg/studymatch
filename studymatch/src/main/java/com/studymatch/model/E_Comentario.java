package com.studymatch.model;

import java.time.LocalDateTime;

public class E_Comentario {

    private String id;
    private String idCalificacion;
    private String texto;
    private String estado; // "Aprobado", "Pendiente de Moderación"
    private String fecha;

    public E_Comentario() {}

    public E_Comentario(String id, String idCalificacion, String texto, String estado) {
        this.id = id;
        this.idCalificacion = idCalificacion;
        this.texto = texto;
        this.estado = estado;
        this.fecha = LocalDateTime.now().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdCalificacion() {
        return idCalificacion;
    }

    public void setIdCalificacion(String idCalificacion) {
        this.idCalificacion = idCalificacion;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
