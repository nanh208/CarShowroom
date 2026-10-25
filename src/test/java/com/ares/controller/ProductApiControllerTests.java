package com.ares.controller;

import com.ares.model.Car;
import com.ares.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductApiControllerTests {
    @Test
    void unknownProductReturnsNotFound() {
        var cars = mock(CarRepository.class);
        when(cars.findById(42L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class,
            () -> new ProductApiController(cars).detail(42L));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void detailMapsExistingDataAndExcludesItselfFromRelated() {
        var cars = mock(CarRepository.class);
        Car car = new Car(); car.setId(1L); car.setName("Test car");
        car.setCategory("RC_CARS"); car.setImage("/images/car.jpg");
        Car other = new Car(); other.setId(2L);
        when(cars.findById(1L)).thenReturn(Optional.of(car));
        when(cars.findByCategory("RC_CARS")).thenReturn(List.of(car, other));
        var result = new ProductApiController(cars).detail(1L);
        var product = (Map<?, ?>) result.get("product");
        assertEquals(List.of("/images/car.jpg"), product.get("images"));
        assertEquals("RC_CARS", product.get("type"));
        assertEquals(List.of(2L), ((List<?>) result.get("related")).stream()
            .map(row -> ((Map<?, ?>) row).get("id")).toList());
        assertEquals(List.of(), result.get("variants"));
        assertFalse(product.containsKey("stock")); // No invented inventory quantity.
    }
}
