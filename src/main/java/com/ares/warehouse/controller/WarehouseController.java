package com.ares.warehouse.controller;

import com.ares.model.User;
import com.ares.warehouse.model.StockMovement;
import com.ares.warehouse.model.WarehouseStock;
import com.ares.warehouse.service.WarehouseService;
import com.ares.warehouse.service.DemandForecastService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/inventory/warehouses")
public class WarehouseController {
    private static final Set<String> MOVEMENT_TYPES = Set.of(
            "PURCHASE_IN", "ADJUSTMENT_IN", "ADJUSTMENT_OUT", "TRANSFER");

    private final WarehouseService service;
    private final DemandForecastService demandForecast;

    public WarehouseController(WarehouseService service, DemandForecastService demandForecast) {
        this.service = service;
        this.demandForecast = demandForecast;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        model.addAttribute("user", (User) session.getAttribute("currentUser"));
        model.addAttribute("warehouses", service.getWarehouses());
        model.addAttribute("items", service.getItems());
        model.addAttribute("stocks", service.getStocks());
        model.addAttribute("movements", service.getRecentMovements());
        Map<Long, Integer> totals = new LinkedHashMap<>();
        service.getWarehouses().forEach(warehouse -> totals.put(warehouse.getId(), 0));
        for (WarehouseStock stock : service.getStocks()) {
            totals.computeIfPresent(stock.getWarehouse().getId(), (id, total) -> total + stock.getQuantity());
        }
        model.addAttribute("totals", totals);
        return "inventory/warehouses";
    }

    @GetMapping("/forecast")
    public String forecast(HttpSession session, Model model,
                           @RequestParam(defaultValue = "90") int days,
                           @RequestParam(defaultValue = "30") int horizon,
                           @RequestParam(defaultValue = "7") int lead,
                           @RequestParam(defaultValue = "0.95") double serviceLevel) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        model.addAttribute("user", (User) session.getAttribute("currentUser"));
        model.addAttribute("warehouses", service.getWarehouses());
        model.addAttribute("suggestions", service.getRestockSuggestions());
        model.addAttribute("report", demandForecast.run(days, horizon, lead, serviceLevel));
        return "inventory/forecast";
    }

    @GetMapping(value = "/forecast.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportForecast(HttpSession session,
                           @RequestParam(defaultValue = "90") int days,
                           @RequestParam(defaultValue = "30") int horizon,
                           @RequestParam(defaultValue = "7") int lead,
                           @RequestParam(defaultValue = "0.95") double serviceLevel) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        DemandForecastService.ForecastReport report = demandForecast.run(days, horizon, lead, serviceLevel);
        StringBuilder csv = new StringBuilder("\uFEFFSKU,Tên mặt hàng,Tồn hiện có,Đã bán trong lịch sử,TB nhu cầu/ngày,Xu hướng/ngày,Nhu cầu thời gian chờ,Nhu cầu kỳ dự báo,Tồn an toàn,Điểm đặt theo nhu cầu,Ngưỡng tồn thủ công,Số lượng đề xuất,Số ngày đủ bán,Ngày dự kiến hết hàng,Mức rủi ro");
        List<String> warehouseCodes = service.getWarehouses().stream().map(warehouse -> warehouse.getCode()).toList();
        warehouseCodes.forEach(code -> csv.append(',').append(csvCell("Tồn " + code)));
        csv.append("\r\n");
        for (DemandForecastService.ForecastItem row : report.items()) {
            csv.append(csvCell(row.sku())).append(',').append(csvCell(row.name())).append(',')
                    .append(row.available()).append(',').append(row.soldInHistory()).append(',')
                    .append(row.avgDailyDemand()).append(',').append(row.trendPerDay()).append(',')
                    .append(row.leadDemand()).append(',').append(row.horizonDemand()).append(',')
                    .append(row.safetyStock()).append(',').append(row.demandReorderPoint()).append(',')
                    .append(row.thresholdReorderPoint()).append(',').append(row.suggestedOrder()).append(',')
                    .append(row.daysOfCover() == null ? "" : row.daysOfCover()).append(',')
                    .append(row.stockoutDate() == null ? "" : row.stockoutDate()).append(',')
                    .append(csvCell(row.risk()));
            for (String code : warehouseCodes) csv.append(',').append(row.byWarehouse().getOrDefault(code, 0));
            csv.append("\r\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=demand-forecast-" + LocalDate.now() + ".csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(body);
    }

    @GetMapping("/movements")
    public String movementReport(HttpSession session, Model model,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Long warehouseId,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "25") int size) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        LocalDate today = LocalDate.now();
        LocalDate start = from == null ? today.minusDays(29) : from;
        LocalDate end = to == null ? today : to;
        boolean invalidRange = start.isAfter(end);
        if (invalidRange) {
            start = today.minusDays(29);
            end = today;
        }
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!MOVEMENT_TYPES.contains(cleanType)) cleanType = "";
        Long cleanWarehouseId = service.getWarehouses().stream()
                .anyMatch(warehouse -> warehouse.getId().equals(warehouseId)) ? warehouseId : null;
        List<StockMovement> rows = filterMovements(service.getMovements(start, end),
                cleanKeyword, cleanType, cleanWarehouseId);
        int pageSize = Set.of(10, 25, 50, 100).contains(size) ? size : 25;
        int pageCount = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int pageStart = Math.min(currentPage * pageSize, rows.size());
        List<StockMovement> visibleRows = rows.subList(pageStart,
                Math.min(pageStart + pageSize, rows.size()));
        model.addAttribute("user", (User) session.getAttribute("currentUser"));
        model.addAttribute("warehouses", service.getWarehouses());
        model.addAttribute("movements", visibleRows);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("type", cleanType);
        model.addAttribute("warehouseId", cleanWarehouseId);
        model.addAttribute("movementTypes", MOVEMENT_TYPES.stream().sorted().toList());
        model.addAttribute("movementCount", rows.size());
        model.addAttribute("page", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageCount", pageCount);
        model.addAttribute("firstResult", rows.isEmpty() ? 0 : pageStart + 1);
        model.addAttribute("lastResult", pageStart + visibleRows.size());
        model.addAttribute("dailySummaries", summarizeByDay(rows));
        model.addAttribute("invalidRange", invalidRange);
        model.addAttribute("inboundQuantity", totalQuantity(rows, MovementDirection.INBOUND));
        model.addAttribute("outboundQuantity", totalQuantity(rows, MovementDirection.OUTBOUND));
        model.addAttribute("transferQuantity", totalQuantity(rows, MovementDirection.TRANSFER));
        return "inventory/movements";
    }

    @GetMapping(value = "/movements.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportMovements(HttpSession session,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Long warehouseId) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        if (from.isAfter(to)) return ResponseEntity.badRequest().build();
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!MOVEMENT_TYPES.contains(cleanType)) cleanType = "";
        Long cleanWarehouseId = service.getWarehouses().stream()
                .anyMatch(warehouse -> warehouse.getId().equals(warehouseId)) ? warehouseId : null;
        List<StockMovement> rows = filterMovements(service.getMovements(from, to),
                cleanKeyword, cleanType, cleanWarehouseId);
        DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder("\uFEFFThời gian,SKU,Mặt hàng,Loại,Từ kho,Đến kho,Số lượng,Ghi chú\r\n");
        for (StockMovement row : rows) {
            csv.append(csvCell(row.getCreatedAt().format(timestamp))).append(',')
                    .append(csvCell(row.getItem().getSku())).append(',')
                    .append(csvCell(row.getItem().getName())).append(',')
                    .append(csvCell(row.getMovementType())).append(',')
                    .append(csvCell(row.getFromWarehouse() == null ? "" : row.getFromWarehouse().getCode())).append(',')
                    .append(csvCell(row.getToWarehouse() == null ? "" : row.getToWarehouse().getCode())).append(',')
                    .append(row.getQuantity()).append(',').append(csvCell(row.getNote())).append("\r\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock-movements-" + from + "-to-" + to + ".csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(body);
    }

    private static List<StockMovement> filterMovements(List<StockMovement> rows, String keyword,
                                                        String type, Long warehouseId) {
        String query = keyword.toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(row -> type.isEmpty() || type.equals(row.getMovementType()))
                .filter(row -> warehouseId == null || belongsToWarehouse(row, warehouseId))
                .filter(row -> query.isEmpty() || contains(row.getItem().getSku(), query)
                        || contains(row.getItem().getName(), query)
                        || contains(row.getNote(), query)
                        || contains(row.getMovementType(), query)
                        || contains(row.getFromWarehouse() == null ? null : row.getFromWarehouse().getCode(), query)
                        || contains(row.getToWarehouse() == null ? null : row.getToWarehouse().getCode(), query))
                .toList();
    }

    private static int totalQuantity(List<StockMovement> rows, MovementDirection direction) {
        return rows.stream().filter(row -> direction.matches(row))
                .mapToInt(StockMovement::getQuantity).sum();
    }

    private static List<DailyMovementSummary> summarizeByDay(List<StockMovement> rows) {
        Map<LocalDate, int[]> totals = new TreeMap<>();
        for (StockMovement row : rows) {
            int[] daily = totals.computeIfAbsent(row.getCreatedAt().toLocalDate(), ignored -> new int[5]);
            daily[0]++;
            daily[1] += row.getQuantity();
            daily[MovementDirection.from(row).summaryIndex()] += row.getQuantity();
        }
        return totals.entrySet().stream()
                .map(entry -> new DailyMovementSummary(entry.getKey(), entry.getValue()[0],
                        entry.getValue()[1], entry.getValue()[2], entry.getValue()[3], entry.getValue()[4]))
                .sorted((left, right) -> right.date().compareTo(left.date()))
                .toList();
    }

    private enum MovementDirection {
        INBOUND(2), OUTBOUND(3), TRANSFER(4);

        private final int summaryIndex;
        MovementDirection(int summaryIndex) { this.summaryIndex = summaryIndex; }

        int summaryIndex() { return summaryIndex; }

        boolean matches(StockMovement row) {
            return switch (this) {
                case INBOUND -> row.getFromWarehouse() == null && row.getToWarehouse() != null;
                case OUTBOUND -> row.getFromWarehouse() != null && row.getToWarehouse() == null;
                case TRANSFER -> row.getFromWarehouse() != null && row.getToWarehouse() != null;
            };
        }

        static MovementDirection from(StockMovement row) {
            if (row.getFromWarehouse() == null && row.getToWarehouse() != null) return INBOUND;
            if (row.getFromWarehouse() != null && row.getToWarehouse() == null) return OUTBOUND;
            return TRANSFER;
        }
    }

    public record DailyMovementSummary(LocalDate date, int movementCount, int totalQuantity,
                                      int inboundQuantity, int outboundQuantity, int transferQuantity) {}

    private static boolean belongsToWarehouse(StockMovement row, Long warehouseId) {
        return row.getFromWarehouse() != null && warehouseId.equals(row.getFromWarehouse().getId())
                || row.getToWarehouse() != null && warehouseId.equals(row.getToWarehouse().getId());
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String csvCell(String value) {
        if (value == null) return "\"\"";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @PostMapping("/items")
    public String addItem(HttpSession session, @RequestParam String sku, @RequestParam String name,
                          @RequestParam(required = false) String category,
                          @RequestParam(defaultValue = "0") int reorderPoint, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.addItem(sku, name, category, reorderPoint), "Đã thêm mặt hàng.", flash);
    }

    @PostMapping("/reorder-point")
    public String updateReorderPoint(HttpSession session, @RequestParam Long itemId,
                                     @RequestParam int reorderPoint, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        try {
            service.updateReorderPoint(itemId, reorderPoint);
            flash.addFlashAttribute("successMessage", "Đã cập nhật ngưỡng nhập hàng.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inventory/warehouses/forecast";
    }

    @PostMapping("/adjust")
    public String adjust(HttpSession session, @RequestParam Long itemId, @RequestParam Long warehouseId,
                         @RequestParam int delta, @RequestParam(required = false) String note,
                         RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.adjust(itemId, warehouseId, delta, note), "Đã cập nhật tồn kho.", flash);
    }

    @PostMapping("/transfer")
    public String transfer(HttpSession session, @RequestParam Long itemId, @RequestParam Long fromId,
                           @RequestParam Long toId, @RequestParam int quantity,
                           @RequestParam(required = false) String note, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.transfer(itemId, fromId, toId, quantity, note), "Đã điều chuyển hàng giữa hai kho.", flash);
    }

    private String perform(Runnable action, String success, RedirectAttributes flash) {
        try {
            action.run();
            flash.addFlashAttribute("successMessage", success);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inventory/warehouses";
    }
}
