package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de materia. Sus consultas están en la implementación JDBC. */
public interface MateriaDAO {
    /** Cuenta las materias cuyo nombre coincide con el texto buscado. */
    Map<String, Object> contarBusqueda(String patron);

    /** Consulta un bloque de resultados de materias. */
    List<Map<String, Object>> buscarPagina(String patron, int desplazamiento);

    /** Lista las materias registradas por el estudiante. */
    List<Map<String, Object>> listarPropias(long estudianteId);

    /** Elimina la relación con una materia solo si pertenece al estudiante. */
    int eliminarPropia(long relacionId, long estudianteId);

    /** Consulta las materias y roles que se muestran en el perfil público. */
    List<Map<String, Object>> listarPublicas(long estudianteId);

    /** Consulta las materias de los estudiantes que se están comparando. */
    List<Map<String, Object>> listarParaComparacion(Object[] parametros);

    /** Registra una materia o actualiza su nivel si ya estaba registrada. */
    int guardarPropia(long estudianteId, long materiaId, String rol, String nivel);

    /** Consulta materias con roles complementarios entre dos estudiantes. */
    List<Map<String, Object>> coincidencias(long estudianteId, long companeroId);

    /** Busca la relación en la que el mentor enseña y el aprendiz aprende. */
    List<Map<String, Object>> validarRoles(long mentorId, long aprendizId, long materiaId);
}
