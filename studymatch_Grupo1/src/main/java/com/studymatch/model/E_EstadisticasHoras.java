package com.studymatch.model;

public class E_EstadisticasHoras {

    private String idEstudiante;
    private double horasEnsenadas;
    private double horasAprendidas;
    private int sesionesCompletadas;
    private double calificacionPromedio;

    public E_EstadisticasHoras() {}

    public E_EstadisticasHoras(String idEstudiante, double horasEnsenadas, double horasAprendidas) {
        this(idEstudiante, horasEnsenadas, horasAprendidas, 0, 5.0);
    }

    public E_EstadisticasHoras(
        String idEstudiante,
        double horasEnsenadas,
        double horasAprendidas,
        int sesionesCompletadas,
        double calificacionPromedio
    ) {
        this.idEstudiante = idEstudiante;
        this.horasEnsenadas = horasEnsenadas;
        this.horasAprendidas = horasAprendidas;
        this.sesionesCompletadas = sesionesCompletadas;
        this.calificacionPromedio = calificacionPromedio;
    }

    public String getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(String idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public double getHorasEnsenadas() {
        return horasEnsenadas;
    }

    public void setHorasEnsenadas(double horasEnsenadas) {
        this.horasEnsenadas = horasEnsenadas;
    }

    public double getHorasAprendidas() {
        return horasAprendidas;
    }

    public void setHorasAprendidas(double horasAprendidas) {
        this.horasAprendidas = horasAprendidas;
    }

    public int getSesionesCompletadas() {
        return sesionesCompletadas;
    }

    public void setSesionesCompletadas(int sesionesCompletadas) {
        this.sesionesCompletadas = sesionesCompletadas;
    }

    public double getCalificacionPromedio() {
        return calificacionPromedio;
    }

    public void setCalificacionPromedio(double calificacionPromedio) {
        this.calificacionPromedio = calificacionPromedio;
    }
}
