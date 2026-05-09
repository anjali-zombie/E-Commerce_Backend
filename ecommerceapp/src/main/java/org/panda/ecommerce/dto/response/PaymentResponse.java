package org.panda.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import org.panda.ecommerce.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private String stripePaymentIntentId;
    private String clientSecret;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime paidAt;
}