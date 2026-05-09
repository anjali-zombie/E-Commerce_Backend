package org.panda.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.panda.ecommerce.dto.request.ProductRequest;
import org.panda.ecommerce.dto.response.PagedResponse;
import org.panda.ecommerce.dto.response.ProductResponse;
import org.panda.ecommerce.exception.ResourceNotFoundException;
import org.panda.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void getAllProducts_shouldReturn200WithPagedResponse() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(1L).name("Widget").price(new BigDecimal("9.99")).stockQuantity(10).active(true).build();

        PagedResponse<ProductResponse> paged = PagedResponse.<ProductResponse>builder()
                .content(List.of(product)).page(0).size(10).totalElements(1).totalPages(1).last(true).build();

        when(productService.getAllProducts(anyInt(), anyInt(), anyString(), anyString())).thenReturn(paged);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Widget"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getProductById_existingProduct_shouldReturn200() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(1L).name("Widget").price(new BigDecimal("9.99")).stockQuantity(5).active(true).build();

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Widget"));
    }

    @Test
    void getProductById_notFound_shouldReturn404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createProduct_validRequest_shouldReturn201() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("New Product");
        request.setPrice(new BigDecimal("49.99"));
        request.setStockQuantity(100);

        ProductResponse response = ProductResponse.builder()
                .id(2L).name("New Product").price(new BigDecimal("49.99")).stockQuantity(100).active(true).build();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("New Product"));
    }

    @Test
    void createProduct_missingName_shouldReturn400() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setPrice(new BigDecimal("9.99"));
        request.setStockQuantity(10);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    void createProduct_negativePrice_shouldReturn400() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Widget");
        request.setPrice(new BigDecimal("-1.00"));
        request.setStockQuantity(10);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
