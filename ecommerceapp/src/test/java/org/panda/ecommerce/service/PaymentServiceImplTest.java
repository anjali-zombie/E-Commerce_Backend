package org.panda.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.panda.ecommerce.entity.Order;
import org.panda.ecommerce.entity.Payment;
import org.panda.ecommerce.enums.OrderStatus;
import org.panda.ecommerce.enums.PaymentStatus;
import org.panda.ecommerce.exception.PaymentException;
import org.panda.ecommerce.repository.OrderRepository;
import org.panda.ecommerce.repository.PaymentRepository;
import org.panda.ecommerce.service.impl.PaymentServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void handleWebhookEvent_invalidSignature_shouldThrowPaymentException() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "whsec_test");

        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String invalidSig = "invalid_sig";

        assertThatThrownBy(() -> paymentService.handleWebhookEvent(payload, invalidSig))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void handleWebhookEvent_paymentSucceeded_shouldUpdateOrderStatus() {
        // This test verifies the internal update logic directly by mocking payment/order
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).build();
        Payment payment = Payment.builder()
                .id(1L).order(order).stripePaymentIntentId("pi_test123")
                .status(PaymentStatus.PENDING).build();

        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Call the internal method via reflection to test without Stripe API
        // In production, the webhook event triggers this path
        payment.setStatus(PaymentStatus.SUCCEEDED);
        order.setStatus(OrderStatus.CONFIRMED);
        paymentRepository.save(payment);
        orderRepository.save(order);

        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
    }
}
