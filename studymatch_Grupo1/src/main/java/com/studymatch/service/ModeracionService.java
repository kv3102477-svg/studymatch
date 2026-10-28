package com.studymatch.service;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
/** Filtro sencillo de palabras. Los mensajes marcados se conservan para revisión, no se publican. */
@Service
public class ModeracionService {
    private final Set<String> palabras=Set.of("idiota","imbecil","estupido");
    public boolean requiereRevision(String texto){
        String normal=Normalizer.normalize(texto,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        return Arrays.stream(normal.split("[^a-z]+")).anyMatch(palabras::contains);
    }
}
