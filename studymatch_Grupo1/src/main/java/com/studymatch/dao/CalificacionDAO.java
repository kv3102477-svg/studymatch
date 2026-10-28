package com.studymatch.dao;

/** Define el acceso a los datos de calificacion. Sus consultas están en la implementación JDBC. */
public interface CalificacionDAO {
    /** Guarda la calificación o actualiza la que ya hizo el participante. */
    long guardar(long sesionId, long estudianteId, int estrellas);

    /** Elimina el comentario anterior de la calificación. */
    int eliminarComentario(long calificacionId);

    /** Guarda el comentario como pendiente de moderación. */
    int guardarComentario(long calificacionId, String comentario);

    /** Marca la sesión como calificada. */
    int marcarSesion(long sesionId);

    /** Cierra el plazo de calificación de sesiones antiguas. */
    int expirarPlazo();
}
