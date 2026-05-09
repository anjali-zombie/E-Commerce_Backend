package org.panda.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.panda.ecommerce.dto.request.PlaceOrderRequest;
import org.panda.ecommerce.dto.response.OrderResponse;
import org.panda.ecommerce.dto.response.PagedResponse;
import org.panda.ecommerce.enums.OrderStatus;
import org.panda.ecommerce.exception.ResourceNotFoundException;
import org.panda.ecommerce.service.OrderService;
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

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void placeOrder_validRequest_shouldReturn201() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setShippingAddress("123 Main St");
        request.setPaymentMethodId("pm_test_123");

        OrderResponse response = OrderResponse.builder()
                .id(1L).userId(1L).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98")).shippingAddress("123 Main St")
                .items(List.of()).build();

        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void placeOrder_missingShippingAddress_shouldReturn400() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setPaymentMethodId("pm_test_123");
        // no shipping address

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_existingOrder_shouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L).userId(1L).status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("29.99")).items(List.of()).build();

        when(orderService.getOrderById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getOrderById_notFound_shouldReturn404() throws Exception {
        when(orderService.getOrderById(99L))
                .thenThrow(new ResourceNotFoundException("Order", "id", 99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyOrders_shouldReturnPagedOrders() throws Exception {
        PagedResponse<OrderResponse> paged = PagedResponse.<OrderResponse>builder()
                .content(List.of()).page(0).size(10).totalElements(0).totalPages(0).last(true).build();

        when(orderService.getMyOrders(anyInt(), anyInt())).thenReturn(paged);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
