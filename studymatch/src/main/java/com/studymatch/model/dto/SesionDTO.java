package com.studymatch.model.dto;

public class SesionDTO {

    private String id;
    private String companeroNombre;
    private String materia;
    private String fecha;
    private String hora;
    private String estado;

    public SesionDTO(
        String id,
        String companeroNombre,
        String materia,
        String fecha,
        String hora,
        String estado
    ) {
        this.id = id;
        this.companeroNombre = companeroNombre;
        this.materia = materia;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public String getCompaneroNombre() {
        return companeroNombre;
    }

    public String getMateria() {
        return materia;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public String getEstado() {
        return estado;
    }
}
