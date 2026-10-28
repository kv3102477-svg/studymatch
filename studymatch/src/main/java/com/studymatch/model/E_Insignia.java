package com.studymatch.model;

public class E_Insignia {

    private String id;
    private String nombre;
    private String descripcion;
    private double metaHoras;
    private String icono;

    public E_Insignia(
        String id,
        String nombre,
        String descripcion,
        double metaHoras,
        String icono
    ) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.metaHoras = metaHoras;
        this.icono = icono;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getMetaHoras() {
        return metaHoras;
    }

    public String getIcono() {
        return icono;
    }
}
