package com.studymatch.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.studymatch.dao.ConversacionDAO;
import com.studymatch.dao.DocumentoDAO;
import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.NotificacionDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Archivos privados en PostgreSQL Large Objects; la tabla conserva sus metadatos. */
@Service
public class DocumentService {

    public static final String FILE_PREFIX = "[STUDYMATCH_ARCHIVO]";

    public record ChatFile(long oid, String nombre, String tipo) {}

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
        new com.fasterxml.jackson.databind.ObjectMapper();
    private final ConversacionDAO conversacionDAO;
    private final DocumentoDAO documentoDAO;
    private final EstudianteDAO estudianteDAO;
    private final NotificacionDAO notificacionDAO;

    public DocumentService(
        ConversacionDAO conversacionDAO,
        DocumentoDAO documentoDAO,
        EstudianteDAO estudianteDAO,
        NotificacionDAO notificacionDAO
    ) {
        this.conversacionDAO = conversacionDAO;
        this.documentoDAO = documentoDAO;
        this.estudianteDAO = estudianteDAO;
        this.notificacionDAO = notificacionDAO;
    }

    private byte[] bytes(MultipartFile file, long max) throws IOException {
        if (file.isEmpty() || file.getSize() > max) throw new ResponseStatusException(
            BAD_REQUEST,
            "El archivo está vacío o supera el tamaño permitido."
        );
        return file.getBytes();
    }

    private String store(byte[] content) {
        long oid = ((Number) documentoDAO.guardarContenido(content).get("oid")).longValue();
        return "pglo:" + oid;
    }

    private long oid(Object value) {
        String locator = Objects.toString(value, "");
        if (!locator.startsWith("pglo:")) throw new ResponseStatusException(
            NOT_FOUND,
            "Archivo no disponible."
        );
        return Long.parseLong(locator.substring(5));
    }

    private void unlink(Object value) {
        if (Objects.toString(value, "").startsWith("pglo:")) documentoDAO.eliminarContenido(
            oid(value)
        );
    }

    /** Valida el archivo y guarda su contenido junto con sus datos. */
    @Transactional(rollbackFor = Exception.class)
    public long upload(long student, MultipartFile file) throws IOException {
        byte[] content = bytes(file, 20L * 1024 * 1024);
        String name = com.studymatch.service.Validaciones.required(
            file.getOriginalFilename(),
            "nombre de archivo",
            255
        ).replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        String format = name.contains(".")
            ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT)
            : "";
        if (!Set.of("PDF", "DOCX", "PPTX").contains(format)) throw new ResponseStatusException(
            BAD_REQUEST,
            "Solo se admiten PDF, DOCX y PPTX."
        );
        if (
            format.equals("PDF") &&
            !(
                content.length >= 5 &&
                new String(content, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals(
                    "%PDF-"
                )
            )
        ) throw new ResponseStatusException(BAD_REQUEST, "El contenido no corresponde a un PDF.");
        if (
            !format.equals("PDF") && !(content.length > 4 && content[0] == 'P' && content[1] == 'K')
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "El contenido no corresponde a un documento de Office."
        );
        return documentoDAO.crear(
            student,
            name,
            format,
            BigDecimal.valueOf(content.length).divide(
                BigDecimal.valueOf(1024 * 1024),
                2,
                RoundingMode.CEILING
            ),
            store(content)
        );
    }

    /** Comprueba la propiedad del documento antes de entregar su contenido. */
    public Map<String, Object> download(long student, long document) {
        var metadata = documentoDAO.buscarPropio(document, student);
        return Map.of(
            "nombre",
            metadata.get("nombre_archivo"),
            "bytes",
            documentoDAO
                .leerContenido(((Number) oid(metadata.get("url_archivo"))).longValue())
                .get("contenido")
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long student, long document) {
        var row = documentoDAO.bloquearPropio(document, student);
        unlink(row.get("url_archivo"));
        documentoDAO.eliminar(document);
    }

    /** Valida y procesa la imagen del perfil antes de guardarla. */
    @Transactional(rollbackFor = Exception.class)
    public void photo(long student, MultipartFile file) throws IOException {
        byte[] content = bytes(file, 5L * 1024 * 1024);
        var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(content));
        if (
            image == null || image.getWidth() > 10000 || image.getHeight() > 10000
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona una imagen válida de hasta 10000 píxeles por lado."
        );
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output);
        if (output.size() > 10 * 1024 * 1024) throw new ResponseStatusException(
            BAD_REQUEST,
            "La imagen es demasiado grande."
        );
        var previous = estudianteDAO.bloquearFoto(student);
        unlink(previous.get("foto_url"));
        estudianteDAO.cambiarFoto(store(output.toByteArray()), student);
    }

    public byte[] photo(long student) {
        Object locator = estudianteDAO.consultarFoto(student).get("foto_url");
        return (byte[]) documentoDAO.leerContenido(oid(locator)).get("contenido");
    }

    public static ChatFile chatFile(String text) {
        if (text == null || !text.startsWith(FILE_PREFIX)) return null;
        try {
            return JSON.readValue(text.substring(FILE_PREFIX.length()), ChatFile.class);
        } catch (IOException e) {
            throw new ResponseStatusException(CONFLICT, "No se pudo leer el archivo adjunto.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public long uploadChat(long student, long conversation, MultipartFile file) throws IOException {
        var chat = conversacionDAO.buscarParticipante(conversation, student);
        byte[] content = bytes(file, 20L * 1024 * 1024);
        String name = com.studymatch.service.Validaciones.required(
            file.getOriginalFilename(),
            "nombre de archivo",
            255
        ).replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        String extension = name.contains(".")
            ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
            : "";
        String type = switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "pdf" -> "application/pdf";
            case "docx", "pptx" -> "application/octet-stream";
            default -> throw new ResponseStatusException(
                BAD_REQUEST,
                "Solo se admiten PDF, DOCX, PPTX, PNG y JPG."
            );
        };
        if (type.startsWith("image/")) {
            try (
                var input = javax.imageio.ImageIO.createImageInputStream(
                    new java.io.ByteArrayInputStream(content)
                )
            ) {
                var readers = javax.imageio.ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new ResponseStatusException(
                    BAD_REQUEST,
                    "La imagen no es válida."
                );
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if (
                        (long) reader.getWidth(0) * reader.getHeight(0) > 25000000
                    ) throw new ResponseStatusException(
                        BAD_REQUEST,
                        "La imagen es demasiado grande."
                    );
                    var image = reader.read(0);
                    var output = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(image, "png", output);
                    content = output.toByteArray();
                    type = "image/png";
                    if (content.length > 20 * 1024 * 1024) throw new ResponseStatusException(
                        BAD_REQUEST,
                        "La imagen es demasiado grande."
                    );
                } finally {
                    reader.dispose();
                }
            }
        } else if (extension.equals("pdf")) {
            if (
                content.length < 5 ||
                !new String(content, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals(
                    "%PDF-"
                )
            ) throw new ResponseStatusException(BAD_REQUEST, "El contenido no es un PDF.");
        } else if (
            content.length < 4 || content[0] != 'P' || content[1] != 'K'
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "El contenido no es un documento de Office."
        );
        long object = oid(store(content));
        String metadata = FILE_PREFIX + JSON.writeValueAsString(new ChatFile(object, name, type));
        long message = conversacionDAO.crearMensaje(conversation, student, metadata);
        long other =
            ((Number) chat.get("estudiante_a")).longValue() == student
                ? ((Number) chat.get("estudiante_b")).longValue()
                : ((Number) chat.get("estudiante_a")).longValue();
        notificacionDAO.crear(
            other,
            "Nuevo archivo en el chat",
            "Tu compañero compartió un archivo.",
            "SISTEMA",
            conversation
        );
        return message;
    }

    public Map<String, Object> chatDownload(long student, long conversation, long message) {
        var row = conversacionDAO.buscarAdjunto(message, conversation, student);
        ChatFile file = chatFile(row.get("texto").toString());
        if (file == null) throw new ResponseStatusException(
            NOT_FOUND,
            "El mensaje no contiene un archivo."
        );
        return Map.of(
            "nombre",
            file.nombre(),
            "tipo",
            file.tipo(),
            "bytes",
            documentoDAO.leerContenido(file.oid()).get("contenido")
        );
    }

    public Object list(long student) {
        return documentoDAO.listarDelEstudiante(student);
    }
}
