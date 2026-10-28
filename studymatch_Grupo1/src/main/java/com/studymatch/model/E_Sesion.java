package com.studymatch.model;

public class E_Sesion {

    private String id;
    private String estudianteProponenteId;
    private String estudianteReceptorId;
    private String materia;
    private String fecha;
    private String hora;
    private String estado;
    private String enlaceVideollamada;
    // RF-026: Registro de Duración
    private Long timestampInicio;
    private Long timestampFin;
    private Integer duracionRealMinutos;
    private String origenDuracion; // "REAL", "ESTIMADO", "REVISADO"
    // RF-028: Finalización de sesión
    private boolean marcaProponente;
    private boolean marcaReceptor;
    // RF-031: Calificación de compañero
    private boolean calificada;
    private String estadoCalificacion = "PENDIENTE"; // "PENDIENTE", "CALIFICADA", "EXPIRADA"

    public E_Sesion() {}

    public E_Sesion(
        String id,
        String estudianteProponenteId,
        String estudianteReceptorId,
        String materia,
        String fecha,
        String hora,
        String estado
    ) {
        this.id = id;
        this.estudianteProponenteId = estudianteProponenteId;
        this.estudianteReceptorId = estudianteReceptorId;
        this.materia = materia;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEstudianteProponenteId() {
        return estudianteProponenteId;
    }

    public void setEstudianteProponenteId(String id) {
        this.estudianteProponenteId = id;
    }

    public String getEstudianteReceptorId() {
        return estudianteReceptorId;
    }

    public void setEstudianteReceptorId(String id) {
        this.estudianteReceptorId = id;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEnlaceVideollamada() {
        return enlaceVideollamada;
    }

    public void setEnlaceVideollamada(String enlaceVideollamada) {
        this.enlaceVideollamada = enlaceVideollamada;
    }

    public Long getTimestampInicio() {
        return timestampInicio;
    }

    public void setTimestampInicio(Long timestampInicio) {
        this.timestampInicio = timestampInicio;
    }

    public Long getTimestampFin() {
        return timestampFin;
    }

    public void setTimestampFin(Long timestampFin) {
        this.timestampFin = timestampFin;
    }

    public Integer getDuracionRealMinutos() {
        return duracionRealMinutos;
    }

    public void setDuracionRealMinutos(Integer duracionRealMinutos) {
        this.duracionRealMinutos = duracionRealMinutos;
    }

    public String getOrigenDuracion() {
        return origenDuracion;
    }

    public void setOrigenDuracion(String origenDuracion) {
        this.origenDuracion = origenDuracion;
    }

    public boolean isMarcaProponente() {
        return marcaProponente;
    }

    public void setMarcaProponente(boolean marcaProponente) {
        this.marcaProponente = marcaProponente;
    }

    public boolean isMarcaReceptor() {
        return marcaReceptor;
    }

    public void setMarcaReceptor(boolean marcaReceptor) {
        this.marcaReceptor = marcaReceptor;
    }

    public boolean isCalificada() {
        return calificada;
    }

    public void setCalificada(boolean calificada) {
        this.calificada = calificada;
    }

    public String getEstadoCalificacion() {
        return estadoCalificacion;
    }

    public void setEstadoCalificacion(String estadoCalificacion) {
        this.estadoCalificacion = estadoCalificacion;
    }
}
