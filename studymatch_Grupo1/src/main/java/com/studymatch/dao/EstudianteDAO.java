package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de estudiante. Sus consultas están en la implementación JDBC. */
public interface EstudianteDAO {
    /** Consulta las credenciales de una cuenta activa para comprobar su sesión. */
    List<Map<String, Object>> validarSesion(long estudianteId);

    /** Obtiene el perfil de una cuenta activa sin incluir la contraseña. */
    Map<String, Object> perfil(long estudianteId);

    /** Desactiva la cuenta indicada. */
    int desactivar(long estudianteId);

    /** Consulta el promedio de calificaciones y el número de compañeros. */
    Map<String, Object> resumen(long estudianteId);

    /** Obtiene los datos que pueden mostrarse a otros estudiantes. */
    Map<String, Object> perfilPublico(long estudianteId);

    /** Consulta el hash usado para comprobar la contraseña. */
    Map<String, Object> claveGuardada(long estudianteId);

    /** Consulta y bloquea la fila antes de reemplazar la foto. */
    Map<String, Object> bloquearFoto(long estudianteId);

    /** Guarda la ubicación de la nueva foto. */
    int cambiarFoto(String ubicacion, long estudianteId);

    /** Obtiene la ubicación de la foto del estudiante. */
    Map<String, Object> consultarFoto(long estudianteId);

    /** Comprueba que la cuenta siga activa y conserve la contraseña esperada. */
    List<Map<String, Object>> validarIdentidad(long estudianteId, String claveHash);

    /** Marca el correo como verificado si las credenciales todavía coinciden. */
    int verificarCorreo(long estudianteId, String claveHash);

    /** Busca una cuenta activa por su correo. */
    List<Map<String, Object>> buscarPorCorreo(String correo);

    /** Reemplaza el hash de contraseña del estudiante. */
    int cambiarClave(String claveHash, long estudianteId);

    /** Guarda un registro nuevo y devuelve su identificador. */
    long crear(String correo, String claveHash, String nombre, String carrera, String ciclo);

    /** Consulta las credenciales necesarias para iniciar sesión. */
    List<Map<String, Object>> credenciales(String correo);

    /** Guarda los campos editables del perfil. */
    int actualizarPerfil(
        String nombre,
        String carrera,
        String ciclo,
        String biografia,
        boolean avisosActivos,
        long estudianteId
    );

    /** Busca compañeros aplicando filtros de materias, roles y horarios. */
    List<Map<String, Object>> buscarCompaneros(
        long estudianteId,
        boolean recomendados,
        String patronBusqueda,
        String carrera,
        String curso,
        String patronCurso,
        String rolCompanero,
        String rolPropio,
        Integer dia
    );

    /** Bloquea temporalmente la fila para coordinar operaciones dentro de una transacción. */
    Map<String, Object> bloquear(long estudianteId);
}
