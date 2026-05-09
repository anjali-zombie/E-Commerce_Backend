package org.panda.ecommerce.service;

import org.panda.ecommerce.dto.request.PlaceOrderRequest;
import org.panda.ecommerce.dto.request.UpdateOrderStatusRequest;
import org.panda.ecommerce.dto.response.OrderResponse;
import org.panda.ecommerce.dto.response.PagedResponse;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse getOrderById(Long id);

    PagedResponse<OrderResponse> getMyOrders(int page, int size);

    PagedResponse<OrderResponse> getAllOrders(int page, int size);

    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);
}
