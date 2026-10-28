package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de documento. Sus consultas están en la implementación JDBC. */
public interface DocumentoDAO {
    /** Consulta los registros asociados al estudiante. */
    List<Map<String, Object>> listarDelEstudiante(long estudianteId);

    /** Guarda el contenido del archivo como objeto grande de PostgreSQL. */
    Map<String, Object> guardarContenido(byte[] contenido);

    /** Libera el objeto grande del archivo en PostgreSQL. */
    Map<String, Object> eliminarContenido(long archivoOid);

    /** Guarda un registro nuevo y devuelve su identificador. */
    long crear(
        long estudianteId,
        String nombre,
        String formato,
        java.math.BigDecimal tamanoMb,
        String ubicacion
    );

    /** Consulta un documento solo si pertenece al estudiante. */
    Map<String, Object> buscarPropio(long documentoId, long estudianteId);

    /** Lee el contenido del archivo a partir de su identificador de almacenamiento. */
    Map<String, Object> leerContenido(long archivoOid);

    /** Consulta y bloquea un documento propio antes de eliminarlo. */
    Map<String, Object> bloquearPropio(long documentoId, long estudianteId);

    /** Elimina los datos del documento indicado. */
    int eliminar(long documentoId);
}
