package com.studymatch.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entidad E_Estudiante para RF-0038: Aviso de Vencimiento.
 * Representa el actor/entidad Estudiante y hereda de E_PerfilEstudiante.
 * Modela los pasos del Diagrama 1:
 * - Paso 6: C_Notificacion -> E_Estudiante: solicitarDatosContacto()
 * - Paso 7: E_Estudiante -> E_Estudiante: consultarCorreosBD()
 * - Paso 8: E_Estudiante -> C_Notificacion: comunicarCorreos()
 */
public class E_Estudiante extends E_PerfilEstudiante {

    public E_Estudiante() {
        super();
    }

    public E_Estudiante(
        String id,
        String correo,
        String contrasena,
        String nombre,
        String carrera,
        String ciclo,
        String bio
    ) {
        super(id, correo, contrasena, nombre, carrera, ciclo, bio);
    }

    /**
     * Paso 6 (Diagrama 1 RF-0038): C_Notificacion -> E_Estudiante: solicitarDatosContacto()
     */
    public static Map<String, String> solicitarDatosContacto(E_PerfilEstudiante estudiante) {
        return consultarCorreosBD(estudiante);
    }

    /**
     * Paso 7 (Diagrama 1 RF-0038): E_Estudiante -> E_Estudiante: consultarCorreosBD()
     */
    public static Map<String, String> consultarCorreosBD(E_PerfilEstudiante estudiante) {
        Map<String, String> datos = new LinkedHashMap<>();
        if (estudiante != null) {
            datos.put("id", estudiante.getId());
            datos.put("nombre", estudiante.getNombre());
            datos.put("correo", estudiante.getCorreo());
            datos.put("carrera", estudiante.getCarrera());
        }
        return datos;
    }

    /**
     * Paso 8 (Diagrama 1 RF-0038): E_Estudiante -> C_Notificacion: comunicarCorreos()
     */
    public static Map<String, String> comunicarCorreos(Map<String, String> datosContacto) {
        return datosContacto;
    }
}
