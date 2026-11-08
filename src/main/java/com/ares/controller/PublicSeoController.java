package com.ares.controller;

import com.ares.model.BlogPost;
import com.ares.model.Car;
import com.ares.repository.BlogPostRepository;
import com.ares.repository.CarRepository;
import com.ares.service.BlogPostService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public sitemap, crawler policy, structured metadata, and lightweight SPA prerender response. */
@RestController
public class PublicSeoController {
    private static final String SITE_NAME = "ARES Toys";
    private static final String DEFAULT_DESCRIPTION = "Xe mô hình die-cast, xe RC, xe trưng bày và phụ kiện chính hãng.";
    private static final Pattern PRODUCT_PATH = Pattern.compile("^/products/(\\d+)$");
    private static final Pattern BLOG_PATH = Pattern.compile("^/blog/([a-z0-9-]{1,100})$");
    private static final Map<String, String[]> STATIC_PAGES = Map.of(
            "/products", new String[]{"Tất cả sản phẩm", "Xe mô hình die-cast, xe RC, xe trưng bày và phụ kiện chính hãng."},
            "/blog", new String[]{"Tin tức & Review", "Tin tức, hướng dẫn và video review xe mô hình, xe RC."},
            "/collections", new String[]{"Bộ sưu tập", "Khám phá các bộ sưu tập xe mô hình được tuyển chọn."},
            "/promotions", new String[]{"Khuyến mãi", "Ưu đãi và khuyến mãi mới nhất tại ARES Toys."},
            "/auctions", new String[]{"Đấu giá hàng hiếm", "Các mẫu xe mô hình hiếm đang được đấu giá."},
            "/faq", new String[]{"Câu hỏi thường gặp", "Thông tin về giao hàng, đổi trả, bảo hành và thanh toán."},
            "/about", new String[]{"Về chúng tôi", DEFAULT_DESCRIPTION},
            "/support", new String[]{"Trung tâm hỗ trợ", "Liên hệ ARES Toys để được hỗ trợ."});

    private final CarRepository cars;
    private final BlogPostRepository posts;
    private final BlogPostService blog;
    private final String baseUrl;
    private final Path staticDist;

    public PublicSeoController(CarRepository cars, BlogPostRepository posts, BlogPostService blog,
            @Value("${app.public-base-url:http://localhost:4173}") String baseUrl,
            @Value("${app.static-dist:frontend/dist}") String staticDist) {
        this.cars = cars;
        this.posts = posts;
        this.blog = blog;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.staticDist = Path.of(staticDist).toAbsolutePath().normalize();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String body = "User-agent: *\nAllow: /\nDisallow: /admin\nDisallow: /account\n"
                + "Disallow: /checkout\nDisallow: /cart\nDisallow: /pay/\nDisallow: /orders\n"
                + "Disallow: /api/\nDisallow: /oauth-callback\n\nSitemap: " + baseUrl + "/sitemap.xml\n";
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600").body(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" ")
                .append("xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        for (String[] page : new String[][]{
                {"/", "daily", "1.0"}, {"/products", "daily", "0.9"}, {"/blog", "daily", "0.8"},
                {"/collections", "weekly", "0.7"}, {"/promotions", "daily", "0.7"},
                {"/auctions", "daily", "0.6"}, {"/bundles", "weekly", "0.6"},
                {"/faq", "monthly", "0.4"}, {"/about", "monthly", "0.3"}, {"/support", "monthly", "0.3"}}) {
            appendUrl(xml, page[0], null, page[1], page[2], null);
        }
        for (Car car : cars.findAll()) {
            appendUrl(xml, "/products/" + car.getId(), null, "weekly", "0.8", absolute(car.getImage()));
        }
        for (BlogPost post : posts.findByStatusOrderByPublishedAtDesc("PUBLISHED")) {
            if (blog.isLive(post)) {
                appendUrl(xml, "/blog/" + post.getSlug(), post.getUpdatedAt(), "monthly", "0.6", absolute(post.getCoverImage()));
            }
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(xml.toString());
    }

    @GetMapping("/api/public/seo/meta")
    public Meta metadata(@RequestParam String path) {
        String safePath = normalizePath(path);
        List<Object> structured = new ArrayList<>();
        Map<String, Object> organization = Map.of("@context", "https://schema.org", "@type", "Organization",
                "name", SITE_NAME, "url", baseUrl, "logo", baseUrl + "/icons/icon-512.png");

        Matcher productMatch = PRODUCT_PATH.matcher(safePath);
        if (productMatch.matches()) {
            Car car = cars.findById(Long.parseLong(productMatch.group(1))).orElse(null);
            if (car != null) return productMetadata(car, safePath, structured);
        }

        Matcher blogMatch = BLOG_PATH.matcher(safePath);
        if (blogMatch.matches()) {
            BlogPost post = posts.findBySlug(blogMatch.group(1)).filter(blog::isLive).orElse(null);
            if (post != null) return blogMetadata(post, safePath, organization, structured);
        }

        if (STATIC_PAGES.containsKey(safePath)) {
            String[] page = STATIC_PAGES.get(safePath);
            structured.add(organization);
            structured.add(breadcrumbs(List.of(new String[]{"Trang chủ", "/"}, new String[]{page[0], safePath})));
            return new Meta(page[0] + " | " + SITE_NAME, page[1], null, "website", absolute(safePath), structured);
        }

        structured.add(organization);
        structured.add(Map.of("@context", "https://schema.org", "@type", "WebSite", "name", SITE_NAME,
                "url", baseUrl, "potentialAction", Map.of("@type", "SearchAction",
                        "target", baseUrl + "/products?q={search_term_string}",
                        "query-input", "required name=search_term_string")));
        return new Meta(SITE_NAME + " — Xe mô hình & xe RC chính hãng", DEFAULT_DESCRIPTION,
                null, "website", absolute(safePath), structured);
    }

    @GetMapping(value = "/api/public/seo/render", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> render(@RequestParam String path) throws java.io.IOException {
        Meta meta = metadata(path);
        Path indexFile = staticDist.resolve("index.html").normalize();
        String html = indexFile.startsWith(staticDist) && Files.isRegularFile(indexFile)
                ? Files.readString(indexFile)
                : "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\"><title>ARES Toys</title></head><body><div id=\"app\"></div></body></html>";
        String image = meta.image() == null ? baseUrl + "/icons/icon-512.png" : meta.image();
        StringBuilder head = new StringBuilder()
                .append("<title>").append(escape(meta.title())).append("</title>\n")
                .append("<meta name=\"description\" content=\"").append(escape(meta.description())).append("\">\n")
                .append("<link rel=\"canonical\" href=\"").append(escape(meta.canonical())).append("\">\n");
        for (String[] tag : new String[][]{
                {"og:title", meta.title()}, {"og:description", meta.description()}, {"og:image", image},
                {"og:url", meta.canonical()}, {"og:type", meta.type()}, {"og:site_name", SITE_NAME}, {"og:locale", "vi_VN"}}) {
            head.append("<meta property=\"").append(tag[0]).append("\" content=\"")
                    .append(escape(tag[1])).append("\">\n");
        }
        head.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        for (Object item : meta.jsonLd()) {
            head.append("<script type=\"application/ld+json\">")
                    .append(json(item).replace("</", "<\\/"))
                    .append("</script>\n");
        }
        html = html.replaceFirst("(?is)<title\\b[^>]*>.*?</title>", "")
                .replaceFirst("(?i)</head>", Matcher.quoteReplacement(head + "</head>"));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=600").body(html);
    }

    private Meta productMetadata(Car car, String path, List<Object> structured) {
        String description = trim(car.getDescription(), 160);
        String image = absolute(car.getImage());
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("@context", "https://schema.org"); product.put("@type", "Product");
        product.put("name", car.getName()); product.put("sku", car.getCode());
        product.put("description", description);
        if (image != null) product.put("image", List.of(image));
        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("@type", "Offer"); offer.put("url", absolute(path)); offer.put("priceCurrency", "VND");
        offer.put("price", car.getPrice());
        offer.put("availability", "https://schema.org/" + (Boolean.TRUE.equals(car.getInStock()) ? "InStock" : "OutOfStock"));
        offer.put("itemCondition", "https://schema.org/NewCondition");
        product.put("offers", offer);
        if (car.getRating() != null && car.getReviewCount() != null && car.getReviewCount() > 0) {
            product.put("aggregateRating", Map.of("@type", "AggregateRating", "ratingValue", car.getRating(), "reviewCount", car.getReviewCount()));
        }
        structured.add(product);
        structured.add(breadcrumbs(List.of(new String[]{"Trang chủ", "/"},
                new String[]{"Sản phẩm", "/products"}, new String[]{car.getName(), path})));
        return new Meta(car.getName() + " | " + SITE_NAME, description, image, "product", absolute(path), structured);
    }

    private Meta blogMetadata(BlogPost post, String path, Map<String, Object> organization, List<Object> structured) {
        String bodyText = org.jsoup.Jsoup.parse(post.getContentHtml() == null ? "" : post.getContentHtml()).text();
        String description = first(post.getSeoDescription(), post.getExcerpt(), trim(bodyText, 160));
        String image = absolute(post.getCoverImage());
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("@context", "https://schema.org");
        article.put("@type", switch (post.getKind()) {
            case "VIDEO" -> "VideoObject";
            case "NEWS" -> "NewsArticle";
            default -> "BlogPosting";
        });
        article.put("name", post.getTitle()); article.put("headline", post.getTitle());
        article.put("description", description);
        if (image != null) article.put("image", List.of(image));
        article.put("datePublished", post.getPublishedAt().toString());
        article.put("dateModified", post.getUpdatedAt().toString());
        article.put("author", Map.of("@type", "Person", "name", post.getAuthor() == null ? SITE_NAME : post.getAuthor()));
        article.put("publisher", organization); article.put("mainEntityOfPage", absolute(path));
        if (post.getVideoUrl() != null) article.put("embedUrl", post.getVideoUrl());
        structured.add(article);
        structured.add(breadcrumbs(List.of(new String[]{"Trang chủ", "/"},
                new String[]{"Tin tức", "/blog"}, new String[]{post.getTitle(), path})));
        String title = first(post.getSeoTitle(), post.getTitle()) + " | " + SITE_NAME;
        return new Meta(title, description, image, "article", absolute(path), structured);
    }

    private Map<String, Object> breadcrumbs(List<String[]> items) {
        List<Map<String, Object>> elements = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            elements.add(Map.of("@type", "ListItem", "position", index + 1,
                    "name", items.get(index)[0], "item", absolute(items.get(index)[1])));
        }
        return Map.of("@context", "https://schema.org", "@type", "BreadcrumbList", "itemListElement", elements);
    }

    private void appendUrl(StringBuilder xml, String path, java.time.LocalDateTime lastModified,
            String frequency, String priority, String image) {
        xml.append("  <url><loc>").append(xmlEscape(absolute(path))).append("</loc>");
        if (lastModified != null) xml.append("<lastmod>").append(lastModified.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>");
        xml.append("<changefreq>").append(frequency).append("</changefreq><priority>").append(priority).append("</priority>");
        if (image != null) xml.append("<image:image><image:loc>").append(xmlEscape(image)).append("</image:loc></image:image>");
        xml.append("</url>\n");
    }

    private String absolute(String path) {
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("https://") || path.startsWith("http://")) return path;
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static String normalizePath(String input) {
        if (input == null || input.isBlank()) return "/";
        String path = input.split("[?#]", 2)[0].trim();
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("\\") || path.contains("..")) return "/";
        if ("/".equals(path) || STATIC_PAGES.containsKey(path) || PRODUCT_PATH.matcher(path).matches()
                || BLOG_PATH.matcher(path).matches()) return path;
        return "/";
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) return DEFAULT_DESCRIPTION;
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) return normalized;
        int cut = normalized.substring(0, maxLength - 1).lastIndexOf(' ');
        return normalized.substring(0, cut > maxLength / 2 ? cut : maxLength - 1) + "…";
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return DEFAULT_DESCRIPTION;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String xmlEscape(String value) { return escape(value); }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return jsonString(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) result.append(',');
                first = false;
                result.append(jsonString(String.valueOf(entry.getKey()))).append(':').append(json(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (value instanceof Collection<?> values) {
            StringBuilder result = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) result.append(',');
                first = false;
                result.append(json(item));
            }
            return result.append(']').toString();
        }
        return jsonString(value.toString());
    }

    private static String jsonString(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) result.append(String.format("\\u%04x", (int) character));
                    else result.append(character);
                }
            }
        }
        return result.append('"').toString();
    }

    public record Meta(String title, String description, String image, String type, String canonical, List<Object> jsonLd) {}
}
