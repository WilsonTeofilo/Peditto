package com.peditto.stores.application.assets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class ImageUploadValidator {
    private static final int MAX_INPUT_BYTES = 5 * 1024 * 1024;
    private static final int MAX_OUTPUT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_DIMENSION = 6_000;
    private static final long MAX_PIXELS = 20_000_000L;

    public SanitizedImage validate(String filename, String declaredType, InputStream input) {
        String extension = extension(filename);
        String normalizedType = declaredType == null ? "" : declaredType.toLowerCase(Locale.ROOT);
        if (!(extension.equals("png") && normalizedType.equals("image/png")
                || (extension.equals("jpg") || extension.equals("jpeg")) && normalizedType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("A imagem deve ser PNG ou JPEG com extensão e MIME compatíveis.");
        }
        try {
            byte[] source = input.readNBytes(MAX_INPUT_BYTES + 1);
            if (source.length == 0 || source.length > MAX_INPUT_BYTES) {
                throw new IllegalArgumentException("Arquivo de imagem excede o limite permitido.");
            }
            try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
                if (imageInput == null) throw new IllegalArgumentException("Conteúdo de imagem inválido.");
                var readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw new IllegalArgumentException("Formato de imagem não suportado.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    boolean matches = extension.equals("png") ? format.equals("png")
                            : format.equals("jpeg") || format.equals("jpg");
                    if (!matches) throw new IllegalArgumentException("O conteúdo não corresponde à extensão declarada.");
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                            || (long) width * height > MAX_PIXELS) {
                        throw new IllegalArgumentException("Dimensões de imagem excedem o limite permitido.");
                    }
                    BufferedImage decoded = reader.read(0);
                    if (decoded == null) throw new IllegalArgumentException("Imagem inválida.");
                    ByteArrayOutputStream normalized = new ByteArrayOutputStream();
                    if (!ImageIO.write(decoded, "png", normalized) || normalized.size() > MAX_OUTPUT_BYTES) {
                        throw new IllegalArgumentException("Imagem não pode ser normalizada com segurança.");
                    }
                    return new SanitizedImage(normalized.toByteArray(), width, height, "image/png");
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler a imagem.", exception);
        }
    }

    private static String extension(String filename) {
        if (filename == null || filename.length() > 255) throw new IllegalArgumentException("Nome de arquivo inválido.");
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) throw new IllegalArgumentException("Extensão de imagem inválida.");
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
