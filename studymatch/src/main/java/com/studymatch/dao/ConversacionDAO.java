package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de conversacion. Sus consultas están en la implementación JDBC. */
public interface ConversacionDAO {
    /** Consulta los registros asociados al estudiante. */
    List<Map<String, Object>> listarDelEstudiante(long estudianteId);

    /** Busca el registro solo si el estudiante pertenece a él. */
    Map<String, Object> buscarParticipante(long conversacionId, long estudianteId);

    /** Guarda un mensaje o la referencia a un archivo del chat. */
    long crearMensaje(long conversacionId, long emisorId, String texto);

    /** Consulta el adjunto si el estudiante participa en la conversación. */
    Map<String, Object> buscarAdjunto(long mensajeId, long conversacionId, long estudianteId);

    /** Busca el chat directo existente entre dos estudiantes. */
    List<Map<String, Object>> buscarDirecta(long estudianteId, long companeroId);

    /** Crea el chat directo de una pareja. */
    long crearDirecta(long estudianteId, long companeroId);

    /** Marca como leídos los mensajes recibidos por el lector. */
    int marcarMensajesLeidos(long primerId, long segundoId, long lectorId);

    /** Reúne los mensajes de la pareja en orden cronológico. */
    List<Map<String, Object>> listarMensajes(long primerId, long segundoId);
}
