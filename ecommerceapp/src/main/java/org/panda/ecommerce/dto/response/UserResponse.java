package org.panda.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Set<String> roles;
    private LocalDateTime createdAt;
}