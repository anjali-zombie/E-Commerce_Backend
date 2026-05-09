package org.panda.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.panda.ecommerce.dto.request.LoginRequest;
import org.panda.ecommerce.dto.request.RegisterRequest;
import org.panda.ecommerce.dto.response.AuthResponse;
import org.panda.ecommerce.entity.Role;
import org.panda.ecommerce.entity.User;
import org.panda.ecommerce.enums.RoleName;
import org.panda.ecommerce.exception.DuplicateResourceException;
import org.panda.ecommerce.repository.RoleRepository;
import org.panda.ecommerce.repository.UserRepository;
import org.panda.ecommerce.security.JwtTokenProvider;
import org.panda.ecommerce.service.impl.AuthServiceImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_newUser_shouldReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password@1");

        Role userRole = new Role(1L, RoleName.ROLE_USER);
        User savedUser = User.builder()
                .id(1L).name("John Doe").email("john@example.com")
                .password("encoded").roles(Set.of(userRole)).build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password@1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenFromEmail("john@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRoles()).contains("ROLE_USER");
    }

    @Test
    void register_duplicateEmail_shouldThrowDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("Password@1");
        request.setName("Test");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void login_validCredentials_shouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password@1");

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "john@example.com", "encoded", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        Role userRole = new Role(1L, RoleName.ROLE_USER);
        User user = User.builder().id(1L).name("John").email("john@example.com")
                .password("encoded").roles(Set.of(userRole)).build();

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("jwt-token");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }
}
