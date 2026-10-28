package com.studymatch.model;

import java.time.LocalDateTime;

public class E_IntencionPago {

    private String id;
    private String idEstudiante;
    private String idPlan;
    private double monto;
    private String estado; // "PENDIENTE", "CANCELADO", "COMPLETADO"
    private LocalDateTime fechaCreacion;

    public E_IntencionPago() {}

    public E_IntencionPago(
        String id,
        String idEstudiante,
        String idPlan,
        double monto,
        String estado
    ) {
        this.id = id;
        this.idEstudiante = idEstudiante;
        this.idPlan = idPlan;
        this.monto = monto;
        this.estado = estado;
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(String idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public String getIdPlan() {
        return idPlan;
    }

    public void setIdPlan(String idPlan) {
        this.idPlan = idPlan;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
