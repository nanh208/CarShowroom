package com.ares.controller;

import com.ares.service.ImageSearchService;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Public visual-search endpoint used by the existing image-search page. */
@RestController
public class ImageSearchController {
    private static final long MAX_IMAGE_BYTES = 6L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "image/gif", "gif");

    private final ImageSearchService imageSearch;

    public ImageSearchController(ImageSearchService imageSearch) {
        this.imageSearch = imageSearch;
    }

    @PostMapping(value = "/api/public/image-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> search(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "8") int limit) {
        if (file == null || file.isEmpty()) return badRequest("Chưa chọn ảnh.");
        if (file.getSize() > MAX_IMAGE_BYTES) return badRequest("Ảnh tối đa 6MB.");

        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null) return badRequest("Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.");
        try {
            byte[] bytes = file.getBytes();
            if (!UploadController.magicMatches(bytes, extension)) {
                return badRequest("Nội dung tệp không khớp với định dạng ảnh.");
            }
            return ResponseEntity.ok(imageSearch.search(bytes, Math.max(1, Math.min(limit, 24))));
        } catch (IOException | IllegalArgumentException error) {
            return badRequest("Không đọc được ảnh (định dạng không hỗ trợ).");
        }
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
