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

    @PostMapping("/items")
    public String addItem(HttpSession session, @RequestParam String sku, @RequestParam String name,
                          @RequestParam(required = false) String category, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.addItem(sku, name, category), "Đã thêm mặt hàng.", flash);
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
