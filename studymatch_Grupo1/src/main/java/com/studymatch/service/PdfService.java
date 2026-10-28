package com.studymatch.service;
import java.io.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.encryption.*;
/** Genera archivos PDF en memoria; nunca utiliza plantillas con datos inventados. */
@Service
public class PdfService {
    public byte[] documento(String titulo,List<String> lineas){return crear(titulo,lineas,null);}
    public byte[] certificado(String titulo,List<String> lineas,String verificacion){return crear(titulo,lineas,verificacion);}
    private byte[] crear(String titulo,List<String> lineas,String verificacion){
        try(var doc=new PDDocument();var salida=new ByteArrayOutputStream()){
            var pagina=new PDPage();doc.addPage(pagina);
            var fuente=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPageContentStream contenido=new PDPageContentStream(doc,pagina);
            float y=740;
            List<String> texto=new ArrayList<>();texto.add(titulo);texto.add("");texto.addAll(lineas);
            for(String linea:texto){
                String pendiente=linea==null?"":linea.replaceAll("[^\u0020-\u00ff]"," ");
                do{
                    int fin=Math.min(86,pendiente.length());
                    if(fin<pendiente.length()){int espacio=pendiente.lastIndexOf(' ',fin);if(espacio>0)fin=espacio;}
                    String parte=pendiente.substring(0,fin);pendiente=pendiente.substring(fin).stripLeading();
                    if(y<55){contenido.close();pagina=new PDPage();doc.addPage(pagina);contenido=new PDPageContentStream(doc,pagina);y=740;}
                    contenido.beginText();contenido.setFont(fuente,11);contenido.newLineAtOffset(50,y);contenido.showText(parte);contenido.endText();y-=17;
                }while(!pendiente.isEmpty());
            }
            if(verificacion!=null){
                try{
                    var matriz=new com.google.zxing.qrcode.QRCodeWriter().encode(verificacion,com.google.zxing.BarcodeFormat.QR_CODE,120,120);
                    var imagen=new java.awt.image.BufferedImage(120,120,java.awt.image.BufferedImage.TYPE_INT_RGB);
                    for(int x=0;x<120;x++)for(int z=0;z<120;z++)imagen.setRGB(x,z,matriz.get(x,z)?0xff000000:0xffffffff);
                    contenido.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc,imagen),50,Math.max(55,y-135),120,120);
                }catch(com.google.zxing.WriterException error){throw new IOException("No se pudo generar el QR.",error);}
            }
            contenido.close();
            var permisos=new AccessPermission();permisos.setCanModify(false);permisos.setCanModifyAnnotations(false);
            var proteccion=new StandardProtectionPolicy(UUID.randomUUID().toString(),"",permisos);
            proteccion.setEncryptionKeyLength(128);doc.protect(proteccion);
            doc.save(salida);return salida.toByteArray();
        }catch(IOException error){throw new java.io.UncheckedIOException("No se pudo generar el PDF.",error);}
    }
}
