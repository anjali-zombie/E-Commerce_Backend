package org.panda.ecommerce.service;

import org.panda.ecommerce.dto.request.LoginRequest;
import org.panda.ecommerce.dto.request.RegisterRequest;
import org.panda.ecommerce.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
