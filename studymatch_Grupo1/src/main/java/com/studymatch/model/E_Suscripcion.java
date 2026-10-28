package com.studymatch.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class E_Suscripcion {

    private String id;
    private String idEstudiante;
    private String correo;
    private String plan; // "Pro-Ciclo" o "Gratuito"
    private String estado; // "TRIAL", "ACTIVO", "GRATUITO", "EXPIRADO"
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private boolean esTrial;

    public E_Suscripcion() {}

    public E_Suscripcion(
        String id,
        String idEstudiante,
        String correo,
        String plan,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        boolean esTrial
    ) {
        this.id = id;
        this.idEstudiante = idEstudiante;
        this.correo = correo;
        this.plan = plan;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.esTrial = esTrial;
    }

    // Métodos representativos del diagrama de secuencias RF-0034
    public boolean consultarHistorial() {
        return this.esTrial;
    }

    public boolean consultarSuscripcionesBD() {
        return this.esTrial;
    }

    public static E_Suscripcion insertarRegistroTrial(
        String id,
        String idEstudiante,
        String correo,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento
    ) {
        return new E_Suscripcion(
            id,
            idEstudiante,
            correo,
            "Pro-Ciclo",
            "TRIAL",
            fechaInicio,
            fechaVencimiento,
            true
        );
    }

    public static E_Suscripcion insertarRegistroBD(
        String id,
        String idEstudiante,
        String correo,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento
    ) {
        return insertarRegistroTrial(id, idEstudiante, correo, fechaInicio, fechaVencimiento);
    }

    // Getters y Setters
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public boolean isEsTrial() {
        return esTrial;
    }

    public void setEsTrial(boolean esTrial) {
        this.esTrial = esTrial;
    }

    public long getDiasRestantes() {
        if (fechaVencimiento == null) return 0;
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        return Math.max(0, dias);
    }

    // Métodos representativos del diagrama de secuencias RF-0035 (Bloqueo por plan ya vigente)
    public boolean isVigente() {
        if (fechaVencimiento == null) return false;
        boolean noVencido = !LocalDate.now().isAfter(fechaVencimiento);
        boolean estadoActivo =
            "TRIAL".equalsIgnoreCase(estado) || "ACTIVO".equalsIgnoreCase(estado);
        return noVencido && estadoActivo && ("Pro-Ciclo".equalsIgnoreCase(plan) || esTrial);
    }

    public boolean consultarSuscripcionActivaBD() {
        return isVigente();
    }

    public boolean retornarEstadoProActivo() {
        return isVigente();
    }

    // ==================== RF-0037: CONSULTA DE SUSCRIPCIÓN ====================
    /**
     * Paso 6 (Diagrama 1 RF-0037): E_Suscripcion -> E_Suscripcion: consultarEstadoYFechas()
     */
    public java.util.Map<String, Object> consultarEstadoYFechas() {
        java.util.Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("id", this.id);
        datos.put("idEstudiante", this.idEstudiante);
        datos.put("correo", this.correo);
        datos.put("plan", this.plan);
        datos.put("estado", this.estado);
        datos.put("fechaInicio", this.fechaInicio != null ? this.fechaInicio.toString() : "");
        datos.put(
            "fechaVencimiento",
            this.fechaVencimiento != null ? this.fechaVencimiento.toString() : ""
        );
        datos.put("esTrial", this.esTrial);
        datos.put("diasRestantes", getDiasRestantes());
        return datos;
    }

    /**
     * Paso 7 (Diagrama 1 RF-0037): E_Suscripcion -> C_Suscripcion: comunicarDatos()
     */
    public java.util.Map<String, Object> comunicarDatos() {
        return consultarEstadoYFechas();
    }

    /**
     * Paso 3 (Diagrama 2 RF-0037): C_Suscripcion -> E_Suscripcion: consultarEstadoSuscripcion()
     */
    public boolean consultarEstadoSuscripcion() {
        return isVigente();
    }

    /**
     * Paso 4 (Diagrama 2 RF-0037): E_Suscripcion -> E_Suscripcion: validarVencimientoPrueba()
     */
    public boolean validarVencimientoPrueba() {
        if (
            this.esTrial &&
            (this.fechaVencimiento == null || LocalDate.now().isAfter(this.fechaVencimiento))
        ) {
            this.estado = "EXPIRADO";
            return true; // caducó
        }
        return false;
    }

    /**
     * Paso 5 (Diagrama 2 RF-0037): E_Suscripcion -> C_Suscripcion: retornarEstadoExpirado()
     */
    public boolean retornarEstadoExpirado() {
        return "EXPIRADO".equalsIgnoreCase(this.estado);
    }

    /**
     * Paso 3 (Diagrama 3 RF-0037): C_Suscripcion -> E_Suscripcion: consultarEstadoBD()
     */
    public boolean consultarEstadoBD() {
        return true;
    }

    /**
     * Paso 4 (Diagrama 3 RF-0037): E_Suscripcion -> E_Suscripcion: detectarFalloConexionBD()
     */
    /**
     * Paso 5 (Diagrama 3 RF-0037): E_Suscripcion -> C_Suscripcion: retornarErrorBD()
     */
    public java.util.Map<String, Object> retornarErrorBD() {
        java.util.Map<String, Object> err = new java.util.LinkedHashMap<>();
        err.put("error", "Error de conexión con la base de datos (Timeout)");
        err.put("desactualizado", true);
        return err;
    }

    // =========================================================================
    // ==================== RF-0038: AVISO DE VENCIMIENTO ======================
    // =========================================================================
    /**
     * Paso 2 (Diagrama 1 RF-0038): C_Suscripcion -> E_Suscripcion: solicitarCuentasPorVencer()
     */
    public static java.util.List<E_Suscripcion> solicitarCuentasPorVencer(
        java.util.List<E_Suscripcion> lista
    ) {
        return consultarRegistrosPorDias(lista);
    }

    /**
     * Paso 3 (Diagrama 1 RF-0038): E_Suscripcion -> E_Suscripcion: consultarRegistrosPorDias()
     * Consulta registros próximos a vencer en los umbrales clave: 15, 7, 3, 1 y 0 días.
     */
    public static java.util.List<E_Suscripcion> consultarRegistrosPorDias(
        java.util.List<E_Suscripcion> lista
    ) {
        java.util.List<E_Suscripcion> afectados = new java.util.ArrayList<>();
        if (lista != null) {
            for (E_Suscripcion s : lista) {
                long dias = s.getDiasRestantes();
                // Considera cuentas activas o trial con días <= 15
                if (
                    ("TRIAL".equalsIgnoreCase(s.getEstado()) ||
                        "ACTIVO".equalsIgnoreCase(s.getEstado())) &&
                    dias <= 15
                ) {
                    afectados.add(s);
                }
            }
        }
        return afectados;
    }

    /**
     * Paso 4 (Diagrama 1 RF-0038): E_Suscripcion -> C_Suscripcion: comunicarListaAfectados()
     */
    public static java.util.List<E_Suscripcion> comunicarListaAfectados(
        java.util.List<E_Suscripcion> afectados
    ) {
        return afectados;
    }

    /**
     * Paso 3 (Diagrama 2 RF-0038): C_Suscripcion -> E_Suscripcion: solicitarUsuariosPorVencer()
     */
    public static java.util.List<E_Suscripcion> solicitarUsuariosPorVencer(
        java.util.List<E_Suscripcion> lista
    ) {
        return consultarRegistrosPorDias(lista);
    }

    /**
     * Paso 4 (Diagrama 2 RF-0038): E_Suscripcion -> E_Suscripcion: detectarUsuarioYaRenovado()
     */
    public boolean detectarUsuarioYaRenovado() {
        // Si el estado es ACTIVO y la fecha de vencimiento es mayor a 30 días, ya renovó
        return "ACTIVO".equalsIgnoreCase(this.estado) && getDiasRestantes() > 30;
    }
    /**
     * Paso 5 (Diagrama 2 RF-0038): E_Suscripcion -> C_Suscripcion: retornarListaExcluyendoRenovados()
     */
}
