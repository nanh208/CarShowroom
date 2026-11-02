package com.ares.service;

import com.ares.model.Car;
import com.ares.model.Category;
import com.ares.repository.CarRepository;
import com.ares.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTests {
    CarRepository cars = mock(CarRepository.class);
    CategoryRepository categories = mock(CategoryRepository.class);
    CatalogService catalog = new CatalogService(cars, categories);

    @BeforeEach void data() {
        Car rc = car(1L, "Xe điều khiển Lamborghini", "RC_CARS", 2000000L, "1:10 Drift", 4.8, true);
        Car model = car(2L, "Ferrari mô hình", "MODEL_CARS", 1000000L, "1:18 Scale", 4.2, true);
        Car unavailable = car(3L, "RC Drift", "RC_CARS", 500000L, "1:10", 3.5, false);
        when(cars.findAll()).thenReturn(List.of(rc, model, unavailable));
        when(categories.findAllByOrderBySortOrderAscCodeAsc()).thenReturn(List.of(
            new Category("RC_CARS", "RC", "TOY", "car", 0),
            new Category("MODEL_CARS", "Model", "TOY", "car", 1),
            new Category("BATTERIES", "Pin", "ACCESSORY", "battery", 2)));
    }

    static Car car(Long id, String name, String category, Long price, String scale, double rating, boolean stock) {
        Car car = new Car(); car.setId(id); car.setName(name); car.setCategory(category);
        car.setCode("SKU-" + id); car.setPrice(price); car.setScale(scale);
        car.setRating(rating); car.setInStock(stock); return car;
    }
    CatalogService.Result find(Map<String, String> query) { return catalog.list(CatalogService.Query.from(query)); }

    @Test void combinesAllFiltersInsteadOfPickingOne() {
        var result = find(Map.of("q", "dieu khien", "category", "RC_CARS", "grp", "TOY", "scale", "1:10",
            "minPrice", "1500000", "maxPrice", "2500000", "minRating", "4.5", "inStock", "true"));
        assertEquals(List.of(1L), result.content().stream().map(Car::getId).toList());
    }
    @Test void incompatibleFiltersReturnNoResults() {
        assertEquals(0, find(Map.of("q", "Ferrari", "category", "RC_CARS")).totalElements());
    }
    @Test void searchesWithoutAccentsAndHandlesOneTypo() {
        assertEquals(2L, find(Map.of("q", "Ferari mo hinh")).content().getFirst().getId());
    }
    @Test void understandsSynonymsScaleAndPriceInQuery() {
        assertEquals(1L, find(Map.of("q", "lambo 1/10 dưới 2,5 triệu")).content().getFirst().getId());
        assertEquals(0, find(Map.of("q", "lambo dưới 1 triệu")).totalElements());
    }
    @Test void paginationHasStableSortAndTotals() {
        var result = find(Map.of("sort", "price_asc", "page", "1", "size", "1"));
        assertEquals(3, result.totalElements()); assertEquals(3, result.totalPages());
        assertEquals(2L, result.content().getFirst().getId());
    }
    @Test void oversizedPageDoesNotOverflow() {
        assertTrue(find(Map.of("page", "2147483647", "size", "60")).content().isEmpty());
    }
    @Test void normalizesBoundsAndCapsPageSize() {
        var result = find(Map.of("page", "-10", "size", "100000"));
        assertEquals(0, result.number()); assertEquals(60, result.size());
    }
    @Test void rejectsInvalidPriceRangeAndRating() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> find(Map.of("minPrice", "20", "maxPrice", "10")));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> find(Map.of("minRating", "NaN")));
    }
    @Test void rejectsBadSortAndTooLongQuery() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> find(Map.of("sort", "sql")));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> find(Map.of("q", "a".repeat(201))));
    }
    @Test void naturalLanguagePriceOverflowIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> find(Map.of("q", "duoi 999999999999999999999 trieu")));
    }
    @Test void categoryCountsIncludeZeroAndScalesAreNormalized() {
        assertEquals(List.of(2L, 1L, 0L), catalog.categories().stream().map(CatalogService.CategoryCount::count).toList());
        assertEquals(List.of("1:10", "1:18"), catalog.scales());
    }
    @Test void stockAndGroupDoNotInventProducts() {
        assertEquals(2, find(Map.of("inStock", "true")).totalElements());
        assertEquals(0, find(Map.of("grp", "ACCESSORY")).totalElements());
    }
    @Test void searchTreatsMarkupAsText() {
        assertEquals(0, find(Map.of("q", "<script>alert(1)</script>")).totalElements());
    }
}
