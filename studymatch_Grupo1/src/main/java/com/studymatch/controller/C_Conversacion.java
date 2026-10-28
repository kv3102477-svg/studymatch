package com.studymatch.controller;

import com.studymatch.service.ConversacionService;
import com.studymatch.service.Validaciones;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Conversacion {

    private final ConversacionService conversacionService;

    public C_Conversacion(ConversacionService conversacionService) {
        this.conversacionService = conversacionService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/conversaciones")
    public Object conversations(HttpSession s) {
        return conversacionService.conversations(student(s));
    }

    @PostMapping("/conversaciones")
    public Object direct(HttpSession s, @RequestBody Map<String, Object> input) {
        return Map.of(
            "id",
            conversacionService.directConversation(
                student(s),
                Validaciones.number(input, "estudiante_id")
            )
        );
    }

    @GetMapping("/conversaciones/{id}/mensajes")
    public Object messages(HttpSession s, @PathVariable long id) {
        return conversacionService.messages(student(s), id);
    }

    @PostMapping("/conversaciones/{id}/mensajes")
    public Object message(
        HttpSession s,
        @PathVariable long id,
        @RequestBody Map<String, Object> input
    ) {
        return Map.of("id", conversacionService.message(student(s), id, input));
    }
}
