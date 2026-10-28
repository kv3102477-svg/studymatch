package com.studymatch.model;

/** Datos del formulario de registro; la contraseña nunca se devuelve al cliente. */
public record Registro(
    String nombre,
    String correo,
    String contrasena,
    String carrera,
    String ciclo
) {}
