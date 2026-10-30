package com.ares.report.controller;

import com.ares.model.Car;
import com.ares.repository.CarRepository;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reports/catalog")
public class CatalogReportController {
    private final CarRepository cars;

    public CatalogReportController(CarRepository cars) { this.cars = cars; }

    @GetMapping
    public String page(HttpSession session, Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String availability) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        List<Car> allCars = cars.findAllByOrderByCategoryAscNameAsc();
        List<Car> filtered = filter(allCars, keyword, category, availability);
        long available = allCars.stream().filter(car -> Boolean.TRUE.equals(car.getInStock())).count();
        long unavailable = allCars.size() - available;
        model.addAttribute("user", session.getAttribute("currentUser"));
        model.addAttribute("cars", filtered);
        model.addAttribute("categories", new TreeSet<>(allCars.stream().map(Car::getCategory)
                .filter(value -> value != null && !value.isBlank()).toList()));
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("category", category == null ? "" : category.trim());
        model.addAttribute("availability", normalizeAvailability(availability));
        model.addAttribute("catalogCount", allCars.size());
        model.addAttribute("availableCount", available);
        model.addAttribute("unavailableCount", unavailable);
        return "reports/catalog";
    }

    @GetMapping(value = "/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(HttpSession session,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String availability) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        StringBuilder csv = new StringBuilder("\uFEFFID,Mã xe,Tên sản phẩm,Danh mục,Khu vực,Tỉ lệ,Chất liệu,Giá (VND),Giá cũ (VND),Giảm giá (%),Tình trạng,Đã bán,Đánh giá,Số lượt đánh giá,Ảnh\r\n");
        for (Car car : filter(cars.findAllByOrderByCategoryAscNameAsc(), keyword, category, availability)) {
            csv.append(car.getId()).append(',').append(csvCell(car.getCode())).append(',')
                    .append(csvCell(car.getName())).append(',').append(csvCell(car.getCategory())).append(',')
                    .append(csvCell(car.getSection())).append(',').append(csvCell(car.getScale())).append(',')
                    .append(csvCell(car.getMaterial())).append(',').append(number(car.getPrice())).append(',')
                    .append(number(car.getOldPrice())).append(',').append(number(car.getDiscountPercent())).append(',')
                    .append(csvCell(Boolean.TRUE.equals(car.getInStock()) ? "Còn hàng" : "Hết hàng")).append(',')
                    .append(number(car.getSoldCount())).append(',').append(number(car.getRating())).append(',')
                    .append(number(car.getReviewCount())).append(',').append(csvCell(car.getImage())).append("\r\n");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=car-catalog.csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static List<Car> filter(List<Car> cars, String keyword, String category, String availability) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = category == null ? "" : category.trim();
        String selectedAvailability = normalizeAvailability(availability);
        return cars.stream()
                .filter(car -> query.isEmpty() || contains(car.getName(), query) || contains(car.getCode(), query))
                .filter(car -> selectedCategory.isEmpty() || selectedCategory.equals(car.getCategory()))
                .filter(car -> selectedAvailability.isEmpty()
                        || ("AVAILABLE".equals(selectedAvailability) == Boolean.TRUE.equals(car.getInStock())))
                .toList();
    }

    private static String normalizeAvailability(String availability) {
        if (availability == null) return "";
        return switch (availability.trim().toUpperCase(Locale.ROOT)) {
            case "AVAILABLE", "OUT" -> availability.trim().toUpperCase(Locale.ROOT);
            default -> "";
        };
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String number(Number value) { return value == null ? "" : value.toString(); }

    private static String csvCell(String value) {
        if (value == null) return "\"\"";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
