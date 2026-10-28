package com.studymatch.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class E_Plan {

    private String id;
    private String nombre;
    private double precio; // S/ 19.90
    private String moneda; // "PEN"
    private String periodo; // "Ciclo Académico (4 meses)"
    private String etiqueta; // "Más Popular"
    private String descripcion;
    private List<String> beneficios;
    private boolean activo;

    public E_Plan() {
        this.beneficios = new ArrayList<>();
    }

    public E_Plan(
        String id,
        String nombre,
        double precio,
        String moneda,
        String periodo,
        String etiqueta,
        String descripcion,
        List<String> beneficios,
        boolean activo
    ) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.moneda = moneda;
        this.periodo = periodo;
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
        this.beneficios = beneficios != null ? beneficios : new ArrayList<>();
        this.activo = activo;
    }

    // Métodos representativos del diagrama de secuencias RF-0035
    public E_Plan consultarPrecioYBeneficios() {
        return this;
    }

    public E_Plan retornarDetallesPlan() {
        return this;
    }

    // ==================== RF-0037: CONSULTA DE SUSCRIPCIÓN ====================
    /**
     * Paso 8 (Diagrama 1 RF-0037): C_Suscripcion -> E_Plan: solicitarDetallePlan()
     */
    public E_Plan solicitarDetallePlan() {
        return this;
    }

    /**
     * Paso 9 (Diagrama 1 RF-0037): E_Plan -> E_Plan: consultarBeneficiosPrecio()
     */
    public Map<String, Object> consultarBeneficiosPrecio() {
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("id", this.id);
        res.put("nombre", this.nombre);
        res.put("precio", this.precio);
        res.put("moneda", this.moneda);
        res.put("periodo", this.periodo);
        res.put("etiqueta", this.etiqueta);
        res.put("descripcion", this.descripcion);
        res.put("beneficios", this.beneficios);
        return res;
    }

    /**
     * Paso 10 (Diagrama 1 RF-0037): E_Plan -> C_Suscripcion: comunicarDetallesPlan()
     */
    public Map<String, Object> comunicarDetallesPlan() {
        return consultarBeneficiosPrecio();
    }

    // Métodos representativos del diagrama de secuencias RF-0036 (Modalidades de Pago)
    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<String> getBeneficios() {
        return beneficios;
    }

    public void setBeneficios(List<String> beneficios) {
        this.beneficios = beneficios;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
