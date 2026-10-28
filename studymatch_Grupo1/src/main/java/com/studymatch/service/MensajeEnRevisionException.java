package com.studymatch.service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
/** Avisa al emisor sin revertir el mensaje retenido para moderación. */
public class MensajeEnRevisionException extends ResponseStatusException {
    public MensajeEnRevisionException(){super(HttpStatus.CONFLICT,"Mensaje bloqueado y marcado para revisión");}
}
