package com.ares.service;

import com.ares.model.Car;
import com.ares.repository.CarRepository;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Ranks catalog cars by visual similarity without persisting the visitor's uploaded image. */
@Service
public class ImageSearchService {
    private record Features(long differenceHash, float[] colorHistogram) {}
    private record IndexedImage(Car car, Features features) {}
    private record Match(Car car, double score) {}

    private final CarRepository cars;
    private final Path staticDirectory;
    private final Path uploadDirectory = Path.of("uploads").toAbsolutePath().normalize();
    private final Map<String, Features> featureCache = new ConcurrentHashMap<>();
    private volatile List<IndexedImage> imageIndex = List.of();

    public ImageSearchService(CarRepository cars,
            @Value("${app.static-dir:frontend/public}") String staticDirectory) {
        this.cars = cars;
        this.staticDirectory = Path.of(staticDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void warmIndex() {
        Thread worker = new Thread(this::reindex, "car-image-index");
        worker.setDaemon(true);
        worker.start();
    }

    /** Rebuilds the catalog image index. Called at startup and lazily if the catalog was empty then. */
    public synchronized int reindex() {
        List<IndexedImage> next = new ArrayList<>();
        for (Car car : cars.findAll()) {
            String image = car.getImage();
            if (image == null || image.isBlank()) continue;
            Features features = featureCache.computeIfAbsent(image, this::readCatalogImage);
            if (features != null) next.add(new IndexedImage(car, features));
        }
        imageIndex = List.copyOf(next);
        return imageIndex.size();
    }

    public Map<String, Object> search(byte[] queryImage, int limit) throws IOException {
        if (queryImage == null || queryImage.length == 0) throw new IllegalArgumentException("Ảnh tìm kiếm đang trống.");
        List<IndexedImage> snapshot = imageIndex;
        if (snapshot.isEmpty()) {
            reindex();
            snapshot = imageIndex;
        }

        Features query = features(queryImage);
        List<Match> matches = new ArrayList<>();
        for (IndexedImage indexed : snapshot) {
            Car car = indexed.car();
            if (!Boolean.TRUE.equals(car.getInStock())) continue;
            matches.add(new Match(car, similarity(query, indexed.features())));
        }
        matches.sort(Comparator.comparingDouble(Match::score).reversed()
                .thenComparing(match -> match.car().getName(), String.CASE_INSENSITIVE_ORDER));

        List<Map<String, Object>> results = matches.stream().limit(limit)
                .map(match -> product(match.car(), match.score())).toList();
        return Map.of("results", results, "indexed", snapshot.size(), "ai", false);
    }

    private Features readCatalogImage(String url) {
        try {
            Path path = resolveCatalogPath(url);
            if (path == null || !Files.isRegularFile(path) || Files.size(path) > 12L * 1024 * 1024) return null;
            return features(Files.readAllBytes(path));
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private Path resolveCatalogPath(String url) {
        String relative;
        Path base;
        if (url.startsWith("/uploads/")) {
            relative = url.substring("/uploads/".length());
            base = uploadDirectory;
        } else if (url.startsWith("/")) {
            relative = url.substring(1);
            base = staticDirectory;
        } else {
            return null; // Remote URLs are not fetched, avoiding arbitrary server-side requests.
        }
        Path resolved = base.resolve(relative).normalize();
        return resolved.startsWith(base) ? resolved : null;
    }

    private static Features features(byte[] bytes) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) throw new IOException("Unsupported image format");
        try {
            BufferedImage gray = scale(source, 9, 8);
            long hash = 0;
            int bit = 0;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    if (luminance(gray.getRGB(x, y)) > luminance(gray.getRGB(x + 1, y))) hash |= 1L << bit;
                    bit++;
                }
            }
            gray.flush();

            BufferedImage color = scale(source, 48, 48);
            float[] histogram = new float[72];
            float totalWeight = 0;
            float[] hsv = new float[3];
            for (int y = 0; y < color.getHeight(); y++) {
                for (int x = 0; x < color.getWidth(); x++) {
                    int rgb = color.getRGB(x, y);
                    Color.RGBtoHSB((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, hsv);
                    if (hsv[1] < 0.12f && hsv[2] > 0.9f) continue; // Ignore common white studio backgrounds.
                    double dx = (x - 23.5) / 24.0;
                    double dy = (y - 23.5) / 24.0;
                    float weight = (float) Math.max(0.25, 1.0 - Math.sqrt(dx * dx + dy * dy));
                    int hue = Math.min(7, (int) (hsv[0] * 8));
                    int saturation = Math.min(2, (int) (hsv[1] * 3));
                    int value = Math.min(2, (int) (hsv[2] * 3));
                    histogram[hue * 9 + saturation * 3 + value] += weight;
                    totalWeight += weight;
                }
            }
            color.flush();
            if (totalWeight > 0) {
                for (int i = 0; i < histogram.length; i++) histogram[i] /= totalWeight;
            }
            return new Features(hash, histogram);
        } finally {
            source.flush();
        }
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static int luminance(int rgb) {
        return (((rgb >>> 16) & 255) * 299 + ((rgb >>> 8) & 255) * 587 + (rgb & 255) * 114) / 1000;
    }

    private static double similarity(Features left, Features right) {
        double histogramIntersection = 0;
        for (int i = 0; i < left.colorHistogram().length; i++) {
            histogramIntersection += Math.min(left.colorHistogram()[i], right.colorHistogram()[i]);
        }
        double shapeSimilarity = 1.0 - Long.bitCount(left.differenceHash() ^ right.differenceHash()) / 64.0;
        return 0.55 * histogramIntersection + 0.45 * shapeSimilarity;
    }

    private static Map<String, Object> product(Car car, double score) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", car.getId());
        result.put("name", car.getName());
        result.put("price", car.getPrice());
        result.put("oldPrice", car.getOldPrice());
        result.put("image", car.getImage());
        result.put("images", List.of(car.getImage()));
        result.put("type", car.getCategory());
        result.put("scale", car.getScale());
        result.put("rating", car.getRating());
        result.put("reviewCount", car.getReviewCount());
        result.put("stock", Boolean.TRUE.equals(car.getInStock()) ? 1 : 0);
        result.put("hasVariants", false);
        result.put("score", Math.round(score * 1000) / 1000.0);
        return result;
    }
}
