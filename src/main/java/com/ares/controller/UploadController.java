package com.ares.controller;

import com.ares.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Stores validated user images and reduces large JPEG/PNG uploads before serving them. */
@RestController
public class UploadController {
    private static final Path UPLOAD_DIR = Path.of("uploads").toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;

    @Value("${app.upload.max-dimension:1920}")
    private int configuredMaxDimension;

    public record Optimized(byte[] bytes, String extension) {}

    @PostMapping(value = "/api/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                      HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("currentUser") instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập trước khi tải ảnh lên."));
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            return badRequest("Ảnh phải có dung lượng từ 1 byte đến 5 MB.");
        }

        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return badRequest("Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.");
        }

        byte[] original = file.getBytes();
        if (original.length > MAX_FILE_BYTES || !magicMatches(original, extension)) {
            return badRequest("Nội dung tệp không khớp với định dạng ảnh đã chọn.");
        }

        byte[] storedBytes = original;
        String storedExtension = extension.equals("jpeg") ? "jpg" : extension;
        if (Set.of("jpg", "jpeg", "png").contains(extension)) {
            Optimized optimized = optimize(original, extension, boundedMaxDimension());
            if (optimized.bytes().length < original.length) {
                storedBytes = optimized.bytes();
                storedExtension = optimized.extension();
            }
        }

        Files.createDirectories(UPLOAD_DIR);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + storedExtension;
        Path target = UPLOAD_DIR.resolve(storedName).normalize();
        if (!target.getParent().equals(UPLOAD_DIR)) throw new IOException("Invalid upload target");
        Files.write(target, storedBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return ResponseEntity.ok(Map.of("url", "/uploads/" + storedName,
                "bytes", Integer.toString(storedBytes.length),
                "originalBytes", Integer.toString(original.length)));
    }

    public static boolean magicMatches(byte[] bytes, String extension) {
        if (bytes == null || bytes.length < 12) return false;
        return switch (extension) {
            case "jpg", "jpeg" -> (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
            case "png" -> (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
            case "gif" -> bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
            case "webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    public static Optimized optimize(byte[] input, String extension, int maxDimension) throws IOException {
        BufferedImage source;
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            if (imageInput == null) throw new IOException("Unable to read uploaded image.");
            Iterator<ImageWriter> unused = ImageIO.getImageWritersByFormatName("jpg");
            if (!unused.hasNext()) throw new IOException("JPEG encoder is unavailable.");
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new IOException("Unsupported or invalid image data.");
            var reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) {
                    throw new IOException("Ảnh vượt giới hạn 25 megapixel.");
                }
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        if (source == null) throw new IOException("Unsupported or invalid image data.");

        double scale = Math.min(1.0, (double) maxDimension / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        boolean transparent = source.getColorModel().hasAlpha() && hasTransparency(source);
        BufferedImage resized = new BufferedImage(width, height,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        var graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (!transparent) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
            source.flush();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (transparent) {
            if (!ImageIO.write(resized, "png", output)) throw new IOException("PNG encoder is unavailable.");
            resized.flush();
            return new Optimized(output.toByteArray(), "png");
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("JPEG encoder is unavailable.");
        ImageWriter writer = writers.next();
        ImageWriteParam parameters = writer.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(0.82f);
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(resized, null, null), parameters);
        } finally {
            writer.dispose();
            resized.flush();
        }
        return new Optimized(output.toByteArray(), "jpg");
    }

    private int boundedMaxDimension() {
        return Math.max(320, Math.min(4096, configuredMaxDimension));
    }

    private static boolean hasTransparency(BufferedImage image) {
        int stepX = Math.max(1, image.getWidth() / 64);
        int stepY = Math.max(1, image.getHeight() / 64);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                if ((image.getRGB(x, y) >>> 24) < 250) return true;
            }
        }
        return false;
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
