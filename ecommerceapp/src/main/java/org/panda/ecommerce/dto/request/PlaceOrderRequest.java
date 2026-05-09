package org.panda.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    // Stripe PaymentMethod ID created on the frontend
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
}