package org.panda.ecommerce.service.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.panda.ecommerce.entity.Order;
import org.panda.ecommerce.entity.Payment;
import org.panda.ecommerce.enums.OrderStatus;
import org.panda.ecommerce.enums.PaymentStatus;
import org.panda.ecommerce.exception.PaymentException;
import org.panda.ecommerce.exception.ResourceNotFoundException;
import org.panda.ecommerce.repository.OrderRepository;
import org.panda.ecommerce.repository.PaymentRepository;
import org.panda.ecommerce.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Override
    @Transactional
    public String createPaymentIntent(Long orderId, BigDecimal amount, String currency, String paymentMethodId) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setReturnUrl("https://your-frontend.com/order-confirmation")
                    .putMetadata("orderId", String.valueOf(orderId))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(() ->
                    Payment.builder()
                            .order(order)
                            .amount(amount)
                            .currency(currency)
                            .build());

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStripeClientSecret(intent.getClientSecret());
            payment.setStatus(mapStripeStatus(intent.getStatus()));
            paymentRepository.save(payment);

            order.setPayment(payment);
            orderRepository.save(order);

            return intent.getClientSecret();

        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent for order {}: {}", orderId, e.getMessage());
            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new PaymentException("Invalid webhook signature");
        }

        log.info("Processing Stripe webhook event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        String intentId = extractPaymentIntentId(event);
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} confirmed after successful payment {}", order.getId(), intentId);
        });
    }

    private void handlePaymentFailed(Event event) {
        String intentId = extractPaymentIntentId(event);
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order {} cancelled after payment failure {}", order.getId(), intentId);
        });
    }

    private String extractPaymentIntentId(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .map(obj -> ((PaymentIntent) obj).getId())
                .orElseThrow(() -> new PaymentException("Could not deserialize PaymentIntent from event"));
    }

    private PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "canceled" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}
