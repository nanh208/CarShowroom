package com.ares.controller;

import com.ares.model.Car;
import com.ares.model.Category;
import com.ares.repository.CarRepository;
import com.ares.repository.CategoryRepository;
import com.ares.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class CatalogWebTests {
    MockMvc mvc;
    @BeforeEach void setup() {
        var cars = mock(CarRepository.class);
        var categories = mock(CategoryRepository.class);
        Car car = new Car(); car.setId(1L); car.setName("Ferrari <script>bad()</script>");
        car.setCode("F-1"); car.setCategory("MODEL_CARS"); car.setPrice(1000000L);
        car.setImage("/images/cars/model-1.jpg"); car.setScale("1:18");
        when(cars.findAll()).thenReturn(List.of(car));
        when(cars.findById(1L)).thenReturn(Optional.of(car));
        when(cars.findByCategory("MODEL_CARS")).thenReturn(List.of(car));
        when(categories.findAllByOrderBySortOrderAscCodeAsc()).thenReturn(List.of(new Category("MODEL_CARS", "Model", "TOY", "car", 0)));
        var catalog = new CatalogService(cars, categories);
        var templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/"); templates.setSuffix(".html"); templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new CatalogPageController(catalog, cars), new ProductApiController(cars, catalog))
            .setViewResolvers(views).build();
    }
    @Test void javaCatalogRendersAndEscapesProductText() throws Exception {
        mvc.perform(get("/catalog")).andExpect(status().isOk())
            .andExpect(content().string(containsString("/catalog/1")))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>bad()"))));
    }
    @Test void javaDetailRendersAndUnknownProductIs404() throws Exception {
        mvc.perform(get("/catalog/1")).andExpect(status().isOk()).andExpect(content().string(containsString("1:18")));
        mvc.perform(get("/catalog/99")).andExpect(status().isNotFound());
    }
    @Test void listApiReturnsPageAndRejectsBadInputs() throws Exception {
        mvc.perform(get("/api/products").param("q", "Ferrari")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].sku").value("F-1"));
        mvc.perform(get("/api/products").param("minPrice", "bad")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("q", "duoi 999999999999999999999 trieu")).andExpect(status().isBadRequest());
    }
    @Test void suggestionAndMetadataAndCategoryEndpointsWork() throws Exception {
        mvc.perform(get("/api/products/suggest").param("q", "Ferrari")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/products/suggest")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/products/search-meta").param("q", "1:18 dưới 2 triệu")).andExpect(status().isOk()).andExpect(jsonPath("$.maxPrice").value(2000000));
        mvc.perform(get("/api/catalog/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].count").value(1));
        mvc.perform(get("/api/catalog/options")).andExpect(status().isOk()).andExpect(jsonPath("$.scales[0]").value("1:18"));
    }
    @Test void emptyPageAndInvalidRangeAreHandled() throws Exception {
        mvc.perform(get("/catalog").param("q", "khongco")).andExpect(status().isOk()).andExpect(content().string(containsString("Chưa tìm thấy")));
        mvc.perform(get("/catalog").param("minPrice", "20").param("maxPrice", "10")).andExpect(status().isBadRequest());
    }
}
