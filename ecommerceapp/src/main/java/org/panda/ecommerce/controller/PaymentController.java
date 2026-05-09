package org.panda.ecommerce.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.panda.ecommerce.dto.response.ApiResponse;
import org.panda.ecommerce.exception.PaymentException;
import org.panda.ecommerce.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Stripe Webhook endpoint.
     * Must receive raw body for signature verification — no authentication filter applied.
     * The Stripe-Signature header is used to verify authenticity.
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse> handleWebhook(HttpServletRequest request) {
        String sigHeader = request.getHeader("Stripe-Signature");

        if (sigHeader == null) {
            log.warn("Stripe webhook received without Stripe-Signature header");
            return ResponseEntity.badRequest().body(ApiResponse.failure("Missing Stripe-Signature header"));
        }

        String payload;
        try {
            payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read webhook payload: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.failure("Failed to read payload"));
        }

        try {
            paymentService.handleWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok(ApiResponse.success("Webhook processed"));
        } catch (PaymentException e) {
            log.warn("Webhook processing rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.failure(e.getMessage()));
        }
    }
}
