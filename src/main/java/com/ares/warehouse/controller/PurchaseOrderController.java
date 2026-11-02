package com.ares.warehouse.controller;

import com.ares.model.User;
import com.ares.warehouse.model.PurchaseOrder;
import com.ares.warehouse.service.PurchaseOrderService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) { this.service = service; }

    @GetMapping
    public String page(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        model.addAttribute("user", (User) session.getAttribute("currentUser"));
        model.addAttribute("warehouses", service.getWarehouses());
        model.addAttribute("items", service.getItems());
        model.addAttribute("orders", service.getOrders());
        return "inventory/purchase-orders";
    }

    @PostMapping
    public String create(HttpSession session, @RequestParam String supplier,
                         @RequestParam Long warehouseId,
                         @RequestParam(required = false) String note,
                         @RequestParam List<Long> itemIds,
                         @RequestParam List<Integer> quantities,
                         @RequestParam List<Long> unitCosts,
                         RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        try {
            PurchaseOrder order = service.create(supplier, warehouseId, note, itemIds, quantities, unitCosts);
            flash.addFlashAttribute("successMessage", "Đã tạo đơn nhập " + order.getCode() + " ở trạng thái nháp.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inventory/purchase-orders";
    }

    @PostMapping("/from-plan")
    public String createFromPlan(HttpSession session, @RequestParam String supplier,
                                 @RequestParam Long warehouseId,
                                 @RequestParam(required = false) String note,
                                 @RequestParam(required = false) List<Long> itemIds,
                                 @RequestParam(required = false) List<Integer> quantities,
                                 @RequestParam(required = false) List<Long> unitCosts,
                                 RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        try {
            PurchaseOrder order = service.create(supplier, warehouseId, note, itemIds, quantities, unitCosts);
            flash.addFlashAttribute("successMessage", "Đã tạo đơn nhập " + order.getCode() + " từ kế hoạch dự báo.");
            return "redirect:/inventory/purchase-orders";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/inventory/warehouses/forecast";
        }
    }

    @PostMapping("/{id}/order")
    public String order(HttpSession session, @PathVariable Long id, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.markOrdered(id), "Đã chuyển đơn nhập sang trạng thái đã đặt.", flash);
    }

    @PostMapping("/{id}/receive")
    public String receive(HttpSession session, @PathVariable Long id, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.receive(id), "Đã nhận hàng và cập nhật tồn kho.", flash);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(HttpSession session, @PathVariable Long id, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.cancel(id), "Đã hủy đơn nhập.", flash);
    }

    private String perform(Runnable action, String success, RedirectAttributes flash) {
        try {
            action.run();
            flash.addFlashAttribute("successMessage", success);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inventory/purchase-orders";
    }
}
