package org.panda.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long JWT_EXPIRATION_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", JWT_EXPIRATION_MS);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        Authentication auth = buildAuthentication("test@example.com");
        String token = jwtTokenProvider.generateToken(auth);
        assertThat(token).isNotBlank();
    }

    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String email = "test@example.com";
        Authentication auth = buildAuthentication(email);
        String token = jwtTokenProvider.generateToken(auth);

        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
    }

    @Test
    void validateToken_validToken_shouldReturnTrue() {
        Authentication auth = buildAuthentication("test@example.com");
        String token = jwtTokenProvider.generateToken(auth);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 1L); // 1 ms
        Authentication auth = buildAuthentication("test@example.com");
        String token = jwtTokenProvider.generateToken(auth);
        Thread.sleep(10);
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_tamperedToken_shouldReturnFalse() {
        Authentication auth = buildAuthentication("test@example.com");
        String token = jwtTokenProvider.generateToken(auth);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void generateTokenFromEmail_shouldReturnValidToken() {
        String token = jwtTokenProvider.generateTokenFromEmail("direct@example.com");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("direct@example.com");
    }

    private Authentication buildAuthentication(String email) {
        UserDetails userDetails = new User(email, "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
