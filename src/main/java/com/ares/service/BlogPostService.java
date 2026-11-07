package com.ares.service;

import com.ares.model.BlogPost;
import com.ares.model.Car;
import com.ares.model.User;
import com.ares.repository.BlogPostRepository;
import com.ares.repository.CarRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Content rules, sanitization, scheduling, and public/admin views for the showroom blog. */
@Service
public class BlogPostService {
    private static final Set<String> KINDS = Set.of("NEWS", "BLOG", "VIDEO");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED");
    private static final Pattern YOUTUBE = Pattern.compile("(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([\\w-]{11})");
    private static final Pattern VIMEO = Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)");
    private static final Safelist RICH_TEXT = Safelist.relaxed()
            .addTags("figure", "figcaption", "hr", "iframe")
            .addAttributes("img", "loading", "width", "height")
            .addAttributes("a", "target", "rel")
            .addAttributes("iframe", "src", "width", "height", "allowfullscreen", "title", "loading")
            .addProtocols("img", "src", "http", "https")
            .addEnforcedAttribute("a", "rel", "noopener nofollow");

    private final BlogPostRepository posts;
    private final CarRepository cars;

    public BlogPostService(BlogPostRepository posts, CarRepository cars) {
        this.posts = posts;
        this.cars = cars;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listPublic(String kind, String tag, String query, int requestedPage, int requestedSize) {
        String normalizedQuery = normalize(query);
        String selectedKind = clean(kind).toUpperCase(Locale.ROOT);
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 30));
        List<BlogPost> matching = published()
                .filter(post -> selectedKind.isBlank() || selectedKind.equals(post.getKind()))
                .filter(post -> clean(tag).isBlank() || post.getTags().contains(clean(tag).toLowerCase(Locale.ROOT)))
                .filter(post -> normalizedQuery.isBlank() || normalize(searchText(post)).contains(normalizedQuery))
                .sorted(Comparator.comparing(BlogPost::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int from = (int) Math.min((long) page * size, matching.size());
        List<Map<String, Object>> content = matching.subList(from, Math.min(from + size, matching.size()))
                .stream().map(this::card).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", matching.size());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> popularTags() {
        Map<String, Long> counts = published().flatMap(post -> post.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(30)
                .map(entry -> Map.<String, Object>of("tag", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    @Transactional
    public Map<String, Object> publicDetail(String slug, boolean countView) {
        BlogPost post = posts.findBySlug(slug).filter(this::isLive)
                .orElseThrow(() -> notFound("Không tìm thấy bài viết."));
        if (countView) {
            post.setViews(post.getViews() + 1);
            posts.save(post);
        }

        Map<String, Object> result = card(post);
        result.put("contentHtml", post.getContentHtml());
        result.put("contentHtmlEn", post.getContentHtmlEn());
        result.put("videoUrl", post.getVideoUrl());
        result.put("seoTitle", post.getSeoTitle());
        result.put("seoDescription", post.getSeoDescription());
        result.put("updatedAt", post.getUpdatedAt());
        result.put("products", cars.findAllById(post.getProductIds()).stream().map(BlogPostService::carCard).toList());
        result.put("related", related(post).stream().map(this::card).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminList() {
        return posts.findAllByOrderByIdDesc().stream().map(post -> {
            Map<String, Object> item = card(post);
            item.put("status", post.getStatus());
            item.put("updatedAt", post.getUpdatedAt());
            return item;
        }).toList();
    }

    @Transactional(readOnly = true)
    public BlogPost adminDetail(Long id) {
        return posts.findById(id).orElseThrow(() -> notFound("Không tìm thấy bài viết."));
    }

    @Transactional
    public BlogPost create(BlogPost input, User author) {
        return save(new BlogPost(), input, author);
    }

    @Transactional
    public BlogPost update(Long id, BlogPost input, User editor) {
        return save(adminDetail(id), input, editor);
    }

    @Transactional
    public void delete(Long id) {
        if (!posts.existsById(id)) throw notFound("Không tìm thấy bài viết.");
        posts.deleteById(id);
    }

    private BlogPost save(BlogPost target, BlogPost input, User author) {
        if (input == null || clean(input.getTitle()).isBlank()) throw badRequest("Tiêu đề bài viết là bắt buộc.");
        if (!KINDS.contains(clean(input.getKind()).toUpperCase(Locale.ROOT))) throw badRequest("Loại bài viết không hợp lệ.");
        if (!STATUSES.contains(clean(input.getStatus()).toUpperCase(Locale.ROOT))) throw badRequest("Trạng thái bài viết không hợp lệ.");
        if (input.getTitle().trim().length() > 180 || length(input.getTitleEn()) > 180
                || length(input.getExcerpt()) > 500 || length(input.getExcerptEn()) > 500
                || length(input.getSeoTitle()) > 100 || length(input.getSeoDescription()) > 300
                || length(input.getCoverImage()) > 2048 || length(input.getContentHtml()) > 500_000
                || length(input.getContentHtmlEn()) > 500_000) {
            throw badRequest("Một hoặc nhiều trường nội dung vượt quá độ dài cho phép.");
        }

        target.setTitle(input.getTitle().trim());
        target.setTitleEn(nullable(input.getTitleEn()));
        target.setExcerpt(nullable(input.getExcerpt()));
        target.setExcerptEn(nullable(input.getExcerptEn()));
        target.setContentHtml(sanitizeHtml(input.getContentHtml()));
        target.setContentHtmlEn(sanitizeHtml(input.getContentHtmlEn()));
        target.setKind(clean(input.getKind()).toUpperCase(Locale.ROOT));
        target.setStatus(clean(input.getStatus()).toUpperCase(Locale.ROOT));

        String cover = nullable(input.getCoverImage());
        if (cover != null && !(cover.startsWith("/") || cover.startsWith("https://"))) {
            throw badRequest("Ảnh bìa phải dùng đường dẫn nội bộ hoặc HTTPS.");
        }
        target.setCoverImage(cover);
        target.setVideoUrl(videoEmbed(input.getVideoUrl()));
        if ("VIDEO".equals(target.getKind()) && target.getVideoUrl() == null) {
            throw badRequest("Bài video cần có liên kết YouTube, Vimeo hoặc tệp video.");
        }
        target.setTags(normalizeTags(input.getTags()));
        target.setProductIds(validProductIds(input.getProductIds()));
        target.setSeoTitle(nullable(input.getSeoTitle()));
        target.setSeoDescription(nullable(input.getSeoDescription()));
        target.setSlug(uniqueSlug(input.getSlug(), target.getTitle(), target.getId()));

        LocalDateTime publishDate = input.getPublishedAt();
        if ("PUBLISHED".equals(target.getStatus()) && target.getPublishedAt() == null && publishDate == null) {
            publishDate = LocalDateTime.now();
        }
        if (publishDate != null) target.setPublishedAt(publishDate);
        if (target.getAuthor() == null && author != null) {
            target.setAuthor(firstNonBlank(author.getFullName(), author.getUsername()));
        }
        if (target.getCreatedAt() == null) target.setCreatedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
        return posts.save(target);
    }

    private Set<String> normalizeTags(Set<String> input) {
        if (input == null || input.isEmpty()) return new LinkedHashSet<>();
        return input.stream().filter(Objects::nonNull).map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isBlank() && tag.length() <= 40)
                .limit(12).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> validProductIds(List<Long> input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();
        List<Long> ids = input.stream().filter(Objects::nonNull).distinct().limit(8).toList();
        Set<Long> existing = cars.findAllById(ids).stream().map(Car::getId).collect(Collectors.toSet());
        return ids.stream().filter(existing::contains).toList();
    }

    private String uniqueSlug(String requested, String title, Long currentId) {
        String base = slugify(clean(requested).isBlank() ? title : requested);
        if (base.isBlank()) base = "bai-viet";
        base = base.length() > 80 ? base.substring(0, 80).replaceAll("-$", "") : base;
        String candidate = base;
        for (int suffix = 2; posts.findBySlug(candidate).filter(found -> !Objects.equals(found.getId(), currentId)).isPresent(); suffix++) {
            String tail = "-" + suffix;
            candidate = base.substring(0, Math.min(base.length(), 95 - tail.length())) + tail;
        }
        return candidate;
    }

    private List<BlogPost> related(BlogPost post) {
        return published().filter(other -> !Objects.equals(other.getId(), post.getId()))
                .sorted(Comparator.<BlogPost>comparingLong(other -> sharedTags(post, other)).reversed()
                        .thenComparing(BlogPost::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3).toList();
    }

    private static long sharedTags(BlogPost left, BlogPost right) {
        return left.getTags().stream().filter(right.getTags()::contains).count();
    }

    private java.util.stream.Stream<BlogPost> published() {
        return posts.findByStatusOrderByPublishedAtDesc("PUBLISHED").stream().filter(this::isLive);
    }

    public boolean isLive(BlogPost post) {
        return "PUBLISHED".equals(post.getStatus()) && post.getPublishedAt() != null
                && !post.getPublishedAt().isAfter(LocalDateTime.now());
    }

    private Map<String, Object> card(BlogPost post) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", post.getId()); item.put("slug", post.getSlug());
        item.put("title", post.getTitle()); item.put("titleEn", post.getTitleEn());
        item.put("excerpt", post.getExcerpt()); item.put("excerptEn", post.getExcerptEn());
        item.put("coverImage", post.getCoverImage()); item.put("kind", post.getKind());
        item.put("tags", post.getTags()); item.put("publishedAt", post.getPublishedAt());
        item.put("author", post.getAuthor()); item.put("views", post.getViews());
        item.put("readMinutes", readMinutes(post.getContentHtml()));
        item.put("hasVideo", post.getVideoUrl() != null);
        return item;
    }

    private static Map<String, Object> carCard(Car car) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", car.getId()); item.put("name", car.getName());
        item.put("price", car.getPrice()); item.put("oldPrice", car.getOldPrice());
        item.put("sku", car.getCode()); item.put("type", car.getCategory());
        item.put("scale", car.getScale()); item.put("rating", car.getRating());
        item.put("reviewCount", car.getReviewCount());
        item.put("stock", Boolean.TRUE.equals(car.getInStock()) ? 1 : 0);
        item.put("hasVariants", false);
        item.put("image", car.getImage());
        item.put("images", car.getImage() == null ? List.of() : List.of(car.getImage()));
        return item;
    }

    public static String sanitizeHtml(String html) {
        if (html == null) return null;
        Document.OutputSettings output = new Document.OutputSettings().prettyPrint(false);
        String sanitized = Jsoup.clean(html, "", RICH_TEXT, output);
        var document = Jsoup.parseBodyFragment(sanitized);
        document.select("iframe").forEach(frame -> {
            String source = frame.attr("src");
            if (!source.matches("^https://(www\\.youtube(-nocookie)?\\.com/embed/|player\\.vimeo\\.com/video/)[\\w\\-?=&]+$")) {
                frame.remove();
            } else {
                frame.attr("loading", "lazy");
            }
        });
        document.select("img").forEach(image -> {
            String source = image.attr("src");
            if (!(source.startsWith("/") || source.startsWith("https://"))) image.remove();
            else image.attr("loading", "lazy");
        });
        document.select("a[href]").forEach(link -> {
            String href = link.attr("href");
            if (!(href.startsWith("/") || href.startsWith("https://") || href.startsWith("http://") || href.startsWith("#"))) {
                link.removeAttr("href");
            }
        });
        return document.body().html();
    }

    private static String videoEmbed(String value) {
        String url = nullable(value);
        if (url == null) return null;
        Matcher youtube = YOUTUBE.matcher(url);
        if (youtube.find()) return "https://www.youtube-nocookie.com/embed/" + youtube.group(1);
        Matcher vimeo = VIMEO.matcher(url);
        if (vimeo.find()) return "https://player.vimeo.com/video/" + vimeo.group(1);
        if (url.matches("^(/|https://)\\S+\\.(mp4|webm)(\\?[^\\s]*)?$") ) return url;
        throw badRequest("Video phải là liên kết YouTube, Vimeo hoặc tệp .mp4/.webm.");
    }

    private static int readMinutes(String html) {
        int words = html == null ? 0 : Jsoup.parse(html).text().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 220.0));
    }

    private static String slugify(String value) {
        String normalized = Normalizer.normalize(clean(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String searchText(BlogPost post) {
        return String.join(" ", clean(post.getTitle()), clean(post.getExcerpt()), String.join(" ", post.getTags()));
    }

    private static String normalize(String text) {
        return Normalizer.normalize(clean(text).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String nullable(String value) { return clean(value).isBlank() ? null : value.trim(); }
    private static int length(String value) { return value == null ? 0 : value.length(); }
    private static String firstNonBlank(String first, String fallback) { return clean(first).isBlank() ? fallback : first.trim(); }
    private static ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
