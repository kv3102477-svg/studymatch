package com.studymatch.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {

    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(
            Map.of(
                "error",
                ex.getReason() == null ? "No se pudo completar la operación." : ex.getReason()
            )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> integrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(
            Map.of("error", "Los datos ya existen o no cumplen las condiciones. Revisa los campos.")
        );
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> database(DataAccessException ex) {
        log.error("Operación de base de datos fallida: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(503).body(
            Map.of(
                "error",
                "No se pudo acceder a los datos. Revisa la conexión y que el esquema esté creado."
            )
        );
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        java.time.DateTimeException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
    })
    ResponseEntity<?> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(
            Map.of("error", "Revisa el formato de los datos enviados.")
        );
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge(Exception ex) {
        return ResponseEntity.status(413).body(
            Map.of("error", "El archivo supera el tamaño permitido.")
        );
    }

    @ExceptionHandler({ org.springframework.mail.MailException.class, java.io.IOException.class })
    ResponseEntity<?> unavailable(Exception ex) {
        return ResponseEntity.status(503).body(
            Map.of("error", "No se pudo completar el envío. Inténtalo de nuevo más tarde.")
        );
    }
}
