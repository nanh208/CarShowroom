package com.ares.service;

import com.ares.dto.ShopDtos.CategoryDto;
import com.ares.model.Category;
import com.ares.repository.CarRepository;
import com.ares.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final CarRepository cars;

    public CategoryService(CategoryRepository categories, CarRepository cars) {
        this.categories = categories;
        this.cars = cars;
    }

    @PostConstruct
    public void seedMissingCategories() {
        var defaults = List.of(
            new Category("RC_CARS", "RC Cars", "TOY", "car", 0),
            new Category("MODEL_CARS", "Model Cars", "TOY", "model", 1),
            new Category("BATTERIES", "Batteries", "ACCESSORY", "battery", 2),
            new Category("CONTROLLERS", "Controllers", "ACCESSORY", "controller", 3),
            new Category("OTHER_PARTS", "Other Parts", "ACCESSORY", "parts", 4)
        );
        for (Category category : defaults) {
            if (!categories.existsByCode(category.code)) categories.save(category);
        }
    }

    public List<CategoryDto> getCategories() {
        return categories.findAllByOrderBySortOrderAscCodeAsc().stream()
            .map(category -> new CategoryDto(category.code, category.name, category.icon,
                Math.toIntExact(cars.countByCategory(category.code))))
            .toList();
    }
}
