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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/inventory/warehouses")
public class WarehouseController {
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
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        LocalDate today = LocalDate.now();
        LocalDate start = from == null ? today.minusDays(29) : from;
        LocalDate end = to == null ? today : to;
        boolean invalidRange = start.isAfter(end);
        if (invalidRange) {
            start = today.minusDays(29);
            end = today;
        }
        List<StockMovement> rows = service.getMovements(start, end);
        model.addAttribute("user", (User) session.getAttribute("currentUser"));
        model.addAttribute("movements", rows);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("invalidRange", invalidRange);
        model.addAttribute("inboundQuantity", rows.stream().filter(row -> row.getFromWarehouse() == null && row.getToWarehouse() != null).mapToInt(StockMovement::getQuantity).sum());
        model.addAttribute("outboundQuantity", rows.stream().filter(row -> row.getFromWarehouse() != null && row.getToWarehouse() == null).mapToInt(StockMovement::getQuantity).sum());
        model.addAttribute("transferQuantity", rows.stream().filter(row -> row.getFromWarehouse() != null && row.getToWarehouse() != null).mapToInt(StockMovement::getQuantity).sum());
        return "inventory/movements";
    }

    @GetMapping(value = "/movements.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportMovements(HttpSession session,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        if (from.isAfter(to)) return ResponseEntity.badRequest().build();
        DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder("\uFEFFThời gian,SKU,Mặt hàng,Loại,Từ kho,Đến kho,Số lượng,Ghi chú\r\n");
        for (StockMovement row : service.getMovements(from, to)) {
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
