package com.ares.service;

import com.ares.model.Car;
import com.ares.repository.CarRepository;
import com.ares.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogScaleTests {
    @Test void scaleOptionsUseNumericOrderAndKeepUnknownLabelsAtEnd() {
        var cars = mock(CarRepository.class);
        when(cars.findAll()).thenReturn(List.of(car("1:18"), car("1:8"), car("1:10 Drift"),
            car("1/8"), car("Special"), car(null)));
        assertEquals(List.of("1:8", "1:10", "1:18", "special"),
            new CatalogService(cars, mock(CategoryRepository.class)).scales());
    }
    private static Car car(String scale) { Car car = new Car(); car.setScale(scale); return car; }
}
