package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de catalogo. Sus consultas están en la implementación JDBC. */
public interface CatalogoDAO {
    /** Consulta el ciclo académico activo más reciente. */
    List<Map<String, Object>> cicloAcademicoActivo();

    /** Lista las carreras disponibles para los formularios. */
    List<Map<String, Object>> carrerasActivas();

    /** Lista los ciclos de estudio disponibles del uno al diez. */
    List<Map<String, Object>> ciclosEstudio();

    /** Consulta una opción de carrera o ciclo usando solo catálogos permitidos. */
    List<Map<String, Object>> buscarOpcion(String catalogo, String nombre);
}
