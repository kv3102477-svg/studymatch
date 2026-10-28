package com.studymatch.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Entidad E_Notificacion para el sistema StudyMatch.
 * Modela los pasos de los diagramas de secuencia de RF-0038 (Aviso de vencimiento):
 * - Diagrama 1:
 *   - Paso 9: C_Notificacion -> E_Notificacion: solicitarRegistroAlertas()
 *   - Paso 10: E_Notificacion -> E_Notificacion: guardarRegistroAlerta()
 *   - Paso 11: E_Notificacion -> C_Notificacion: comunicarExitoRegistro()
 *   - Paso 20: C_Notificacion -> E_Notificacion: solicitarRegistrosAlertas()
 *   - Paso 21: E_Notificacion -> C_Notificacion: devolverAlertaInApp()
 * - Diagrama 3:
 *   - Paso 3: C_Notificacion -> E_Notificacion: registrarIntentoAlerta()
 *   - Paso 4: E_Notificacion -> C_Notificacion: confirmarGuardadoLocal()
 */
public class E_Notificacion {

    private String id;
    private String receptorId;
    private String titulo;
    private String mensaje;
    private String tipo; // PUSH, CORREO, IN_APP
    private boolean leida;
    private String estadoEnvio; // ENVIADO, PENDIENTE_REINTENTO, GUARDADO_LOCAL, FALLIDO
    private long diasAviso; // 15, 7, 3, 1, 0
    private String fechaCreacion;
    private String canal; // CORREO, IN_APP, AMBOS

    public E_Notificacion() {
        this.fechaCreacion = LocalDateTime.now().toString();
        this.estadoEnvio = "ENVIADO";
    }

    public E_Notificacion(
        String id,
        String receptorId,
        String titulo,
        String mensaje,
        String tipo
    ) {
        this.id = id;
        this.receptorId = receptorId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.leida = false;
        this.estadoEnvio = "ENVIADO";
        this.fechaCreacion = LocalDateTime.now().toString();
        this.canal = tipo;
        this.diasAviso = 7;
    }

    public E_Notificacion(
        String id,
        String receptorId,
        String titulo,
        String mensaje,
        String tipo,
        long diasAviso,
        String canal
    ) {
        this.id = id;
        this.receptorId = receptorId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.leida = false;
        this.estadoEnvio = "ENVIADO";
        this.fechaCreacion = LocalDateTime.now().toString();
        this.diasAviso = diasAviso;
        this.canal = canal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceptorId() {
        return receptorId;
    }

    public void setReceptorId(String receptorId) {
        this.receptorId = receptorId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public long getDiasAviso() {
        return diasAviso;
    }

    public void setDiasAviso(long diasAviso) {
        this.diasAviso = diasAviso;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    // =========================================================================
    // MÉTODOS DEL DIAGRAMA DE SECUENCIAS RF-0038
    // =========================================================================
    /**
     * Paso 9 (Diagrama 1 RF-0038): C_Notificacion -> E_Notificacion: solicitarRegistroAlertas()
     */
    public static boolean solicitarRegistroAlertas(
        List<E_Notificacion> repo,
        E_Notificacion nuevaAlerta
    ) {
        return guardarRegistroAlerta(repo, nuevaAlerta);
    }

    /**
     * Paso 10 (Diagrama 1 RF-0038): E_Notificacion -> E_Notificacion: guardarRegistroAlerta()
     */
    public static boolean guardarRegistroAlerta(
        List<E_Notificacion> repo,
        E_Notificacion nuevaAlerta
    ) {
        if (repo != null && nuevaAlerta != null) {
            repo.add(nuevaAlerta);
            return true;
        }
        return false;
    }

    /**
     * Paso 11 (Diagrama 1 RF-0038): E_Notificacion -> C_Notificacion: comunicarExitoRegistro()
     */
    public static Map<String, Object> comunicarExitoRegistro(E_Notificacion alerta) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("exito", true);
        resp.put("idAlerta", alerta != null ? alerta.getId() : "");
        resp.put("estado", "REGISTRADO");
        resp.put("timestamp", LocalDateTime.now().toString());
        return resp;
    }

    /**
     * Paso 20 (Diagrama 1 RF-0038): C_Notificacion -> E_Notificacion: solicitarRegistrosAlertas()
     */
    public static List<E_Notificacion> solicitarRegistrosAlertas(
        List<E_Notificacion> lista,
        String idEstudiante
    ) {
        List<E_Notificacion> noLeidas = new ArrayList<>();
        if (lista != null) {
            for (E_Notificacion n : lista) {
                if (n.getReceptorId().equals(idEstudiante) && !n.isLeida()) {
                    noLeidas.add(n);
                }
            }
        }
        return noLeidas;
    }

    /**
     * Paso 21 (Diagrama 1 RF-0038): E_Notificacion -> C_Notificacion: devolverAlertaInApp()
     */
    public static Optional<E_Notificacion> devolverAlertaInApp(
        List<E_Notificacion> lista,
        String idEstudiante
    ) {
        List<E_Notificacion> noLeidas = solicitarRegistrosAlertas(lista, idEstudiante);
        if (!noLeidas.isEmpty()) {
            return Optional.of(noLeidas.get(noLeidas.size() - 1));
        }
        return Optional.empty();
    }

    /**
     * Paso 3 (Diagrama 3 RF-0038): C_Notificacion -> E_Notificacion: registrarIntentoAlerta()
     */
    public static E_Notificacion registrarIntentoAlerta(
        List<E_Notificacion> repo,
        String idEstudiante,
        String titulo,
        String mensaje,
        long diasRestantes
    ) {
        String nuevoId = "notif-intento-" + UUID.randomUUID().toString().substring(0, 8);
        E_Notificacion intento = new E_Notificacion(
            nuevoId,
            idEstudiante,
            titulo,
            mensaje,
            "CORREO",
            diasRestantes,
            "CORREO_SMTP"
        );
        intento.setEstadoEnvio("GUARDADO_LOCAL");
        if (repo != null) {
            repo.add(intento);
        }
        return intento;
    }

    /**
     * Paso 4 (Diagrama 3 RF-0038): E_Notificacion -> C_Notificacion: confirmarGuardadoLocal()
     */
    public static Map<String, Object> confirmarGuardadoLocal(E_Notificacion intento) {
        Map<String, Object> confirmacion = new LinkedHashMap<>();
        confirmacion.put("guardadoLocal", true);
        confirmacion.put("idIntento", intento != null ? intento.getId() : "");
        confirmacion.put("estadoEnvio", "GUARDADO_LOCAL");
        confirmacion.put(
            "mensaje",
            "Alerta persistida en almacenamiento local previo al despacho SMTP."
        );
        return confirmacion;
    }
}
