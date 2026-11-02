package com.ares.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CatalogAvailabilityTests {
    @Test void stockFilterAcceptsValuesUsedInStorefrontUrls() {
        for (String value : new String[]{"1", "true", " TRUE "})
            assertTrue(CatalogService.Query.from(Map.of("inStock", value)).inStock());
        for (String value : new String[]{"0", "false", ""})
            assertFalse(CatalogService.Query.from(Map.of("inStock", value)).inStock());
    }
    @Test void malformedStockValueDoesNotSilentlyDisableFilter() {
        assertThrows(ResponseStatusException.class,
            () -> CatalogService.Query.from(Map.of("inStock", "tru")));
    }
}
