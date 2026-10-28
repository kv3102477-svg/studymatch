package com.studymatch.dao;

import java.util.Map;

/** Define el acceso a los datos de token. Sus consultas están en la implementación JDBC. */
public interface TokenDAO {
    /** Invalida los tokens anteriores de recuperación de la cuenta. */
    int invalidarRecuperacion(long estudianteId);

    /** Guarda un token con su fecha de vencimiento. */
    int crearRecuperacion(long estudianteId, String codigoHash);

    /** Busca un token no utilizado, vigente y asociado a una cuenta activa. */
    Map<String, Object> buscarRecuperacionVigente(String codigoHash);
}
