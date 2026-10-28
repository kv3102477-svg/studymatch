package com.studymatch.dao;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de disponibilidad. Sus consultas están en la implementación JDBC. */
public interface DisponibilidadDAO {
    /** Consulta los registros correspondientes al estudiante. */
    List<Map<String, Object>> listar(long estudianteId);

    /** Consulta el horario que se muestra en el perfil público. */
    List<Map<String, Object>> listarPublica(long estudianteId);

    /** Busca registros que se superponen con el intervalo indicado. */
    List<Map<String, Object>> buscarCruces(
        long estudianteId,
        int dia,
        LocalTime fin,
        LocalTime inicio
    );

    /** Guarda un registro nuevo y devuelve su identificador. */
    int crear(long estudianteId, int dia, LocalTime inicio, LocalTime fin);

    /** Elimina los bloques anteriores del horario del estudiante. */
    int eliminarDelEstudiante(long estudianteId);

    /** Guarda varios bloques horarios en una misma operación. */
    void guardarBloques(List<Object[]> bloques);

    /** Consulta los bloques de un día, ordenados por hora de inicio. */
    List<Map<String, Object>> bloquesDelDia(long estudianteId, int dia);
}
