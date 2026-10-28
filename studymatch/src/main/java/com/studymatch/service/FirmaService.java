package com.studymatch.service;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
/** Firma documentos de la aplicación. No suplanta una firma oficial de la universidad. */
@Service
public class FirmaService {
    private final Path ruta;
    private KeyPair claves;
    @Autowired
    public FirmaService(@Value("${studymatch.certificados.clave:.keys/certificados.pk8}") String ruta){this(Path.of(ruta));}
    public FirmaService(Path ruta){this.ruta=ruta;}
    private synchronized KeyPair claves(){
        if(claves!=null)return claves;
        try{
            Path publica=Path.of(ruta+".pub");
            var fabrica=KeyFactory.getInstance("Ed25519");
            if(Files.exists(ruta)&&Files.exists(publica)){
                claves=new KeyPair(fabrica.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(publica))),
                    fabrica.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(ruta))));
            }else{
                if(Files.exists(ruta)||Files.exists(publica))throw new IOException("El par de claves está incompleto.");
                claves=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
                Files.createDirectories(ruta.toAbsolutePath().getParent());
                Files.write(ruta,claves.getPrivate().getEncoded(),StandardOpenOption.CREATE_NEW);
                Files.write(publica,claves.getPublic().getEncoded(),StandardOpenOption.CREATE_NEW);
                ruta.toFile().setReadable(false,false);ruta.toFile().setReadable(true,true);
                ruta.toFile().setWritable(false,false);ruta.toFile().setWritable(true,true);
            }
            return claves;
        }catch(GeneralSecurityException|IOException error){throw new IllegalStateException("No se pudieron cargar las claves de firma.",error);}
    }
    public byte[] firmar(byte[] contenido){
        try{
            var firma=Signature.getInstance("Ed25519");firma.initSign(claves().getPrivate());firma.update(contenido);return firma.sign();
        }catch(GeneralSecurityException error){throw new IllegalStateException("No se pudo firmar el documento.",error);}
    }
    public byte[] clavePublica(){return claves().getPublic().getEncoded();}
    public boolean verificar(byte[] contenido,byte[] firma,byte[] publica){
        if(contenido==null||firma==null||publica==null||!Arrays.equals(publica,clavePublica()))return false;
        try{
            var verificador=Signature.getInstance("Ed25519");verificador.initVerify(claves().getPublic());verificador.update(contenido);return verificador.verify(firma);
        }catch(GeneralSecurityException error){return false;}
    }
}
