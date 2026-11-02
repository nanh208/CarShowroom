package com.ares.service;

import com.ares.repository.CarRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class CarSeedTests {
    @Test
    void startupDoesNotDeleteOrReseedExistingProducts() {
        var cars = mock(CarRepository.class);
        when(cars.count()).thenReturn(1L);
        new CarService(cars).initSeedData();
        verify(cars, never()).deleteAll();
        verify(cars, never()).saveAll(any());
    }

    @Test
    void emptyCatalogStillReceivesDemoProducts() {
        var cars = mock(CarRepository.class);
        new CarService(cars).initSeedData();
        verify(cars).saveAll(argThat(rows -> {
            int count = 0;
            for (var ignored : rows) count++;
            return count == 9;
        }));
        verify(cars, never()).deleteAll();
    }
}
