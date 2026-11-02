package com.ares.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogSearchPriceTests {
    @Test void repeatedPriceLimitsUseTheirIntersectionInEitherOrder() {
        assertEquals(1000000L, CatalogSearch.parse("dưới 1 triệu dưới 3 triệu").maxPrice());
        assertEquals(1000000L, CatalogSearch.parse("dưới 3 triệu dưới 1 triệu").maxPrice());
        assertEquals(3000000L, CatalogSearch.parse("trên 3 triệu từ 1 triệu").minPrice());
        assertEquals(3000000L, CatalogSearch.parse("từ 1 triệu trên 3 triệu").minPrice());
    }
    @Test void contradictoryNaturalPriceLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CatalogSearch.parse("trên 3 triệu dưới 1 triệu"));
    }
}
