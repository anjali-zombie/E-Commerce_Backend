package org.panda.ecommerce.service;

public interface PaymentService {

    String createPaymentIntent(Long orderId, java.math.BigDecimal amount, String currency, String paymentMethodId);

    void handleWebhookEvent(String payload, String sigHeader);
}
