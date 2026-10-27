package com.ares.service;

import com.ares.model.Car;
import com.ares.model.Category;
import com.ares.repository.CarRepository;
import com.ares.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CarRepository cars;
    private final CategoryRepository categories;
    public static final Set<String> SORTS = Set.of("featured", "newest", "price_asc", "price_desc", "name", "rating", "bestseller");

    public CatalogService(CarRepository cars, CategoryRepository categories) {
        this.cars = cars; this.categories = categories;
    }

    public record Query(String q, String category, String grp, String scale,
                        Long minPrice, Long maxPrice, Double minRating, Boolean inStock,
                        String sort, int page, int size) {
        public static Query from(Map<String, String> params) {
            try {
                return new Query(params.get("q"), params.getOrDefault("category", params.get("type")),
                    params.get("grp"), params.get("scale"), money(params.get("minPrice")), money(params.get("maxPrice")),
                    blank(params.get("minRating")) ? null : Double.valueOf(params.get("minRating")),
                    Boolean.valueOf(params.getOrDefault("inStock", "false")), params.get("sort"),
                    blank(params.get("page")) ? 0 : Integer.parseInt(params.get("page")),
                    blank(params.get("size")) ? 12 : Integer.parseInt(params.get("size")));
            } catch (IllegalArgumentException error) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Bộ lọc không hợp lệ: " + error.getMessage());
            }
        }
        private static boolean blank(String value) { return value == null || value.isBlank(); }
        private static Long money(String value) { return blank(value) ? null : Long.valueOf(value); }
        public Query {
            q = q == null ? "" : q.trim();
            category = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
            grp = grp == null ? "" : grp.trim().toUpperCase(Locale.ROOT);
            scale = scale == null ? "" : CatalogSearch.scale(scale);
            sort = sort == null || sort.isBlank() ? "featured" : sort;
            page = Math.max(0, page); size = Math.max(1, Math.min(size, 60));
            if (q.length() > 200) throw new IllegalArgumentException("Từ khóa tối đa 200 ký tự");
            if (minPrice != null && minPrice < 0 || maxPrice != null && maxPrice < 0
                    || minPrice != null && maxPrice != null && minPrice > maxPrice)
                throw new IllegalArgumentException("Khoảng giá không hợp lệ");
            if (minRating != null && (!Double.isFinite(minRating) || minRating < 0 || minRating > 5))
                throw new IllegalArgumentException("Đánh giá phải từ 0 đến 5");
            if (!SORTS.contains(sort)) throw new IllegalArgumentException("Kiểu sắp xếp không hợp lệ");
        }
    }

    public record Result(List<Car> content, long totalElements, int totalPages, int number, int size) {}
    public record CategoryCount(String code, String name, String grp, String image, long count) {}

    public List<CategoryCount> categories() {
        Map<String, Long> counts = new HashMap<>();
        for (Car car : cars.findAll()) if (car.getCategory() != null) counts.merge(car.getCategory(), 1L, Long::sum);
        return categories.findAllByOrderBySortOrderAscCodeAsc().stream()
            .map(c -> new CategoryCount(c.code, c.name, c.grp, c.image, counts.getOrDefault(c.code, 0L))).toList();
    }

    public List<String> scales() {
        return cars.findAll().stream().map(Car::getScale).filter(Objects::nonNull)
            .map(CatalogSearch::scale).filter(s -> !s.isBlank()).distinct().sorted().toList();
    }

    public Result list(Query query) {
        var parsed = CatalogSearch.parse(query.q());
        Set<String> groupCodes = new HashSet<>();
        if (!query.grp().isEmpty()) for (Category category : categories.findAllByOrderBySortOrderAscCodeAsc())
            if (query.grp().equalsIgnoreCase(category.grp)) groupCodes.add(category.code);
        var rows = cars.findAll().stream()
            .filter(c -> query.category().isEmpty() || Arrays.stream(query.category().split(",")).anyMatch(code -> code.trim().equals(c.getCategory())))
            .filter(c -> query.grp().isEmpty() || groupCodes.contains(c.getCategory()))
            .filter(c -> query.scale().isEmpty() || query.scale().equals(CatalogSearch.scale(c.getScale())))
            .filter(c -> query.minPrice() == null || c.getPrice() != null && c.getPrice() >= query.minPrice())
            .filter(c -> query.maxPrice() == null || c.getPrice() != null && c.getPrice() <= query.maxPrice())
            .filter(c -> query.minRating() == null || c.getRating() != null && c.getRating() >= query.minRating())
            .filter(c -> !Boolean.TRUE.equals(query.inStock()) || Boolean.TRUE.equals(c.getInStock()))
            .filter(c -> CatalogSearch.matches(c, parsed)).sorted(order(query.sort())).toList();
        int from = (int) Math.min((long) query.page() * query.size(), rows.size());
        return new Result(rows.subList(from, Math.min(from + query.size(), rows.size())), rows.size(),
            (int) ((rows.size() + (long) query.size() - 1) / query.size()), query.page(), query.size());
    }

    private static Comparator<Car> order(String sort) {
        Comparator<Car> comparator = switch (sort) {
            case "price_asc" -> Comparator.comparing(Car::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "price_desc" -> Comparator.comparing(Car::getPrice, Comparator.nullsLast(Comparator.reverseOrder()));
            case "name" -> Comparator.comparing(c -> CatalogSearch.normalize(c.getName()));
            case "rating" -> Comparator.comparing(Car::getRating, Comparator.nullsLast(Comparator.reverseOrder()));
            case "newest" -> Comparator.comparing(Car::getId, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(Car::getSoldCount, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        return comparator.thenComparing(Car::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
