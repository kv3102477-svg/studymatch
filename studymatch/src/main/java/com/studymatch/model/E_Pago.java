package com.studymatch.model;

import java.time.LocalDateTime;

public class E_Pago {

    private String id;
    private String idEstudiante;
    private String idPlan;
    private String modalidad; // "SEMESTRAL" o "MENSUAL"
    private double monto;
    private String moneda;
    private String ultimosDigitosTarjeta;
    private String marcaTarjeta;
    private String titular;
    private String codigoAutorizacion;
    private String estado; // "COMPLETADO", "RECHAZADO", "TIMEOUT"
    private String motivoFallo;
    private String numeroComprobante;
    private LocalDateTime fechaPago;

    public E_Pago() {}

    public E_Pago(
        String id,
        String idEstudiante,
        String idPlan,
        String modalidad,
        double monto,
        String moneda,
        String ultimosDigitosTarjeta,
        String marcaTarjeta,
        String titular,
        String codigoAutorizacion,
        String estado,
        String motivoFallo,
        String numeroComprobante
    ) {
        this.id = id;
        this.idEstudiante = idEstudiante;
        this.idPlan = idPlan;
        this.modalidad = modalidad;
        this.monto = monto;
        this.moneda = moneda;
        this.ultimosDigitosTarjeta = ultimosDigitosTarjeta;
        this.marcaTarjeta = marcaTarjeta;
        this.titular = titular;
        this.codigoAutorizacion = codigoAutorizacion;
        this.estado = estado;
        this.motivoFallo = motivoFallo;
        this.numeroComprobante = numeroComprobante;
        this.fechaPago = LocalDateTime.now();
    }

    // Métodos representativos del diagrama de secuencias RF-0036
    public static E_Pago solicitarRegistroTransaccion(
        String id,
        String idEstudiante,
        String idPlan,
        String modalidad,
        double monto,
        String ultimosDigitos,
        String marca,
        String titular,
        String authCode,
        String numComprobante
    ) {
        return new E_Pago(
            id,
            idEstudiante,
            idPlan,
            modalidad,
            monto,
            "PEN",
            ultimosDigitos,
            marca,
            titular,
            authCode,
            "COMPLETADO",
            null,
            numComprobante
        );
    }

    public void insertarRegistroCompletado() {
        this.estado = "COMPLETADO";
    }

    public boolean comunicarRegistroExitoso() {
        return "COMPLETADO".equals(this.estado);
    }

    // ==================== RF-0037: CONSULTA DE SUSCRIPCIÓN ====================
    /**
     * Paso 11 (Diagrama 1 RF-0037): C_Suscripcion -> E_Pago: solicitarHistorialPagos()
     */
    public static java.util.List<E_Pago> solicitarHistorialPagos(
        java.util.List<E_Pago> listaPagos,
        String idEstudiante
    ) {
        return consultarTransacciones(listaPagos, idEstudiante);
    }

    /**
     * Paso 12 (Diagrama 1 RF-0037): E_Pago -> E_Pago: consultarTransacciones()
     */
    public static java.util.List<E_Pago> consultarTransacciones(
        java.util.List<E_Pago> listaPagos,
        String idEstudiante
    ) {
        java.util.List<E_Pago> trans = new java.util.ArrayList<>();
        if (listaPagos != null) {
            for (E_Pago p : listaPagos) {
                if (p.getIdEstudiante().equals(idEstudiante)) {
                    trans.add(p);
                }
            }
        }
        return trans;
    }

    /**
     * Paso 13 (Diagrama 1 RF-0037): E_Pago -> C_Suscripcion: comunicarHistorial()
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("idPago", this.id);
        map.put("idEstudiante", this.idEstudiante);
        map.put("idPlan", this.idPlan);
        map.put("modalidad", this.modalidad);
        map.put("monto", this.monto);
        map.put("moneda", this.moneda);
        map.put("ultimosDigitosTarjeta", this.ultimosDigitosTarjeta);
        map.put("marcaTarjeta", this.marcaTarjeta);
        map.put("titular", this.titular);
        map.put("codigoAutorizacion", this.codigoAutorizacion);
        map.put("estado", this.estado);
        map.put("motivoFallo", this.motivoFallo);
        map.put("numeroComprobante", this.numeroComprobante);
        map.put("fechaPago", this.fechaPago != null ? this.fechaPago.toString() : "");
        return map;
    }

    public static java.util.List<java.util.Map<String, Object>> comunicarHistorial(
        java.util.List<E_Pago> lista
    ) {
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        if (lista != null) {
            for (E_Pago p : lista) {
                resultado.add(p.toMap());
            }
        }
        return resultado;
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

    public String getIdPlan() {
        return idPlan;
    }

    public void setIdPlan(String idPlan) {
        this.idPlan = idPlan;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getUltimosDigitosTarjeta() {
        return ultimosDigitosTarjeta;
    }

    public void setUltimosDigitosTarjeta(String ultimosDigitosTarjeta) {
        this.ultimosDigitosTarjeta = ultimosDigitosTarjeta;
    }

    public String getMarcaTarjeta() {
        return marcaTarjeta;
    }

    public void setMarcaTarjeta(String marcaTarjeta) {
        this.marcaTarjeta = marcaTarjeta;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }

    public void setCodigoAutorizacion(String codigoAutorizacion) {
        this.codigoAutorizacion = codigoAutorizacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void setMotivoFallo(String motivoFallo) {
        this.motivoFallo = motivoFallo;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }
}
