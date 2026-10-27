package com.ares.service;

import com.ares.model.Category;
import com.ares.repository.CarRepository;
import com.ares.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTests {
    @Test
    void preservesExistingCategoriesWhenSeeding() {
        var categories = mock(CategoryRepository.class);
        when(categories.existsByCode(anyString())).thenReturn(true);
        new CategoryService(categories, mock(CarRepository.class)).seedMissingCategories();
        verify(categories, never()).save(any());
    }

    @Test
    void reportsActualCountsIncludingEmptyCategories() {
        var categories = mock(CategoryRepository.class);
        var cars = mock(CarRepository.class);
        when(categories.findAllByOrderBySortOrderAscCodeAsc()).thenReturn(List.of(
            new Category("RC_CARS", "RC Cars", "TOY", "car", 0),
            new Category("BATTERIES", "Batteries", "ACCESSORY", "battery", 1)));
        when(cars.countByCategory("RC_CARS")).thenReturn(4L);
        var result = new CategoryService(categories, cars).getCategories();
        assertEquals(List.of("RC_CARS", "BATTERIES"), result.stream().map(c -> c.getCode()).toList());
        assertEquals(4, result.get(0).getCount());
        assertEquals(0, result.get(1).getCount());
    }
}
