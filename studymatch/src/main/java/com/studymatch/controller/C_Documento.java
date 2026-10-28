package com.studymatch.controller;

import com.studymatch.service.DocumentService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class C_Documento {

    private final DocumentService files;

    public C_Documento(DocumentService files) {
        this.files = files;
    }

    private long id(HttpSession s) {
        return (Long) s.getAttribute("studentId");
    }

    @GetMapping("/documentos")
    public Object list(HttpSession s) {
        return files.list(id(s));
    }

    @PostMapping("/documentos")
    public Object upload(HttpSession s, @RequestParam MultipartFile archivo) throws IOException {
        return Map.of("id", files.upload(id(s), archivo));
    }

    @GetMapping("/documentos/{document}")
    public ResponseEntity<byte[]> download(HttpSession s, @PathVariable long document) {
        var result = files.download(id(s), document);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(result.get("nombre").toString(), StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body((byte[]) result.get("bytes"));
    }

    @DeleteMapping("/documentos/{document}")
    public Object delete(HttpSession s, @PathVariable long document) {
        files.delete(id(s), document);
        return Map.of("mensaje", "Documento eliminado.");
    }

    @PostMapping("/perfil/foto")
    public Object photo(HttpSession s, @RequestParam MultipartFile archivo) throws IOException {
        files.photo(id(s), archivo);
        return Map.of("mensaje", "Foto guardada.");
    }

    @GetMapping(value = "/perfil/foto", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] photo(HttpSession s) {
        return files.photo(id(s));
    }

    @PostMapping("/conversaciones/{conversation}/archivos")
    public Object uploadChat(
        HttpSession s,
        @PathVariable long conversation,
        @RequestParam MultipartFile archivo
    ) throws IOException {
        return Map.of("id", files.uploadChat(id(s), conversation, archivo));
    }

    @GetMapping("/conversaciones/{conversation}/mensajes/{message}/archivo")
    public ResponseEntity<byte[]> chatDownload(
        HttpSession s,
        @PathVariable long conversation,
        @PathVariable long message
    ) {
        var file = files.chatDownload(id(s), conversation, message);
        String type = file.get("tipo").toString();
        var response = ResponseEntity.ok().contentType(MediaType.parseMediaType(type));
        if (!type.startsWith("image/")) response.header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.get("nombre").toString(), StandardCharsets.UTF_8)
                .build()
                .toString()
        );
        return response.body((byte[]) file.get("bytes"));
    }
}
