package org.panda.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.panda.ecommerce.enums.OrderStatus;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}