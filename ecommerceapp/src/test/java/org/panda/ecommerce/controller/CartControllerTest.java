package org.panda.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.panda.ecommerce.dto.request.AddToCartRequest;
import org.panda.ecommerce.dto.response.CartResponse;
import org.panda.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@Import(TestSecurityConfig.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private CartResponse emptyCart() {
        return CartResponse.builder()
                .id(1L).userId(1L).items(List.of())
                .totalAmount(BigDecimal.ZERO).totalItems(0).build();
    }

    @Test
    void getCart_shouldReturn200() throws Exception {
        when(cartService.getCart()).thenReturn(emptyCart());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void addItem_validRequest_shouldReturn200() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartService.addItem(any(AddToCartRequest.class))).thenReturn(emptyCart());

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_zeroQuantity_shouldReturn400() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(0);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearCart_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
