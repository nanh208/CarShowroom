package com.ares.controller;

import com.ares.repository.CarRepository;
import com.ares.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Controller
public class CatalogPageController {
    private final CatalogService catalog;
    private final CarRepository cars;
    public CatalogPageController(CatalogService catalog, CarRepository cars) {
        this.catalog = catalog; this.cars = cars;
    }

    @ResponseBody
    @GetMapping("/api/catalog/categories")
    public java.util.List<CatalogService.CategoryCount> categories() { return catalog.categories(); }

    @ResponseBody
    @GetMapping("/api/catalog/options")
    public Map<String, Object> options() { return Map.of("scales", catalog.scales(), "sorts", CatalogService.SORTS); }

    @GetMapping("/catalog")
    public String list(@RequestParam Map<String, String> params, Model model) {
        var query = CatalogService.Query.from(params);
        try { model.addAttribute("result", catalog.list(query)); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()); }
        model.addAttribute("filter", query);
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("scales", catalog.scales());
        return "catalog/list";
    }

    @GetMapping("/catalog/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var car = cars.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        model.addAttribute("car", car);
        model.addAttribute("related", car.getCategory() == null ? java.util.List.of() :
            cars.findByCategory(car.getCategory()).stream().filter(c -> !id.equals(c.getId())).limit(4).toList());
        return "catalog/detail";
    }
}
