package org.panda.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import org.panda.ecommerce.dto.request.PlaceOrderRequest;
import org.panda.ecommerce.dto.request.UpdateOrderStatusRequest;
import org.panda.ecommerce.dto.response.OrderItemResponse;
import org.panda.ecommerce.dto.response.OrderResponse;
import org.panda.ecommerce.dto.response.PagedResponse;
import org.panda.ecommerce.dto.response.PaymentResponse;
import org.panda.ecommerce.entity.*;
import org.panda.ecommerce.exception.BadRequestException;
import org.panda.ecommerce.exception.ResourceNotFoundException;
import org.panda.ecommerce.exception.UnauthorizedException;
import org.panda.ecommerce.exception.InsufficientStockException;
import org.panda.ecommerce.repository.*;
import org.panda.ecommerce.service.CartService;
import org.panda.ecommerce.service.OrderService;
import org.panda.ecommerce.service.PaymentService;
import org.panda.ecommerce.util.AppConstants;
import org.panda.ecommerce.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final CartService cartService;

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty. Add products before placing an order."));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty. Add products before placing an order.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate stock and decrement
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is no longer available.");
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity(), cartItem.getQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .productName(product.getName())
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .build();
            orderItems.add(orderItem);
            total = total.add(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .shippingAddress(request.getShippingAddress())
                .build();
        order = orderRepository.save(order);

        // Link order to items
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);
        order = orderRepository.save(order);

        // Create Stripe PaymentIntent (async - frontend confirms with client secret)
        String clientSecret = paymentService.createPaymentIntent(
                order.getId(), total, AppConstants.STRIPE_CURRENCY, request.getPaymentMethodId());

        // Clear the user's cart
        cartService.clearCart();

        Order finalOrder = orderRepository.findByIdWithItems(order.getId())
                .orElse(order);
        return toResponse(finalOrder, clientSecret);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this order");
        }
        return toResponse(order, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyOrders(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        Page<Order> orders = orderRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPagedResponse(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        Page<Order> orders = orderRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPagedResponse(orders);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        order.setStatus(request.getStatus());
        return toResponse(orderRepository.save(order), null);
    }

    private OrderResponse toResponse(Order order, String clientSecret) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            paymentResponse = PaymentResponse.builder()
                    .id(payment.getId())
                    .stripePaymentIntentId(payment.getStripePaymentIntentId())
                    .clientSecret(clientSecret != null ? clientSecret : payment.getStripeClientSecret())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(payment.getStatus())
                    .paidAt(payment.getPaidAt())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .items(itemResponses)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .payment(paymentResponse)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private PagedResponse<OrderResponse> toPagedResponse(Page<Order> page) {
        return PagedResponse.<OrderResponse>builder()
                .content(page.getContent().stream().map(o -> toResponse(o, null)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
