package com.ares.warehouse.controller;

import com.ares.model.User;
import com.ares.warehouse.model.WarehouseStock;
import com.ares.warehouse.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/inventory/warehouses")
public class WarehouseController {
    private final WarehouseService service;

    public WarehouseController(WarehouseService service) { this.service = service; }

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
    public String forecast(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        model.addAttribute("user", (User) session.getAttribute("currentUser"));
        model.addAttribute("suggestions", service.getRestockSuggestions());
        return "inventory/forecast";
    }

    @GetMapping(value = "/forecast.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportForecast(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        StringBuilder csv = new StringBuilder("\uFEFFSKU,Tên mặt hàng,Danh mục,Tồn hiện có,Ngưỡng tồn tối thiểu,Số lượng đề xuất");
        List<String> warehouseCodes = service.getWarehouses().stream().map(warehouse -> warehouse.getCode()).toList();
        warehouseCodes.forEach(code -> csv.append(',').append(csvCell("Tồn " + code)));
        csv.append("\r\n");
        for (WarehouseService.RestockSuggestion row : service.getRestockSuggestions()) {
            csv.append(csvCell(row.sku())).append(',').append(csvCell(row.name())).append(',')
                    .append(csvCell(row.category())).append(',').append(row.available()).append(',')
                    .append(row.reorderPoint()).append(',').append(row.suggestedOrder());
            for (String code : warehouseCodes) csv.append(',').append(row.byWarehouse().getOrDefault(code, 0));
            csv.append("\r\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=restock-plan-" + LocalDate.now() + ".csv")
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
