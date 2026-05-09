package org.panda.ecommerce.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.panda.ecommerce.entity.Category;
import org.panda.ecommerce.entity.Product;
import org.panda.ecommerce.entity.Role;
import org.panda.ecommerce.entity.User;
import org.panda.ecommerce.enums.RoleName;
import org.panda.ecommerce.repository.CategoryRepository;
import org.panda.ecommerce.repository.ProductRepository;
import org.panda.ecommerce.repository.RoleRepository;
import org.panda.ecommerce.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedUsers();
        seedCategoriesAndProducts();
        log.info("Dev data initialized successfully");
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, RoleName.ROLE_USER));
            roleRepository.save(new Role(null, RoleName.ROLE_ADMIN));
            log.info("Roles seeded");
        }
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("admin@ecommerce.com")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();

            userRepository.save(User.builder()
                    .name("Admin User")
                    .email("admin@ecommerce.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .roles(Set.of(adminRole, userRole))
                    .build());

            userRepository.save(User.builder()
                    .name("Test User")
                    .email("user@ecommerce.com")
                    .password(passwordEncoder.encode("User@1234"))
                    .roles(Set.of(userRole))
                    .build());

            log.info("Default users seeded (admin@ecommerce.com / Admin@1234)");
        }
    }

    private void seedCategoriesAndProducts() {
        if (categoryRepository.count() == 0) {
            Category electronics = categoryRepository.save(Category.builder()
                    .name("Electronics")
                    .description("Electronic gadgets and accessories")
                    .build());

            Category clothing = categoryRepository.save(Category.builder()
                    .name("Clothing")
                    .description("Men and women apparel")
                    .build());

            productRepository.save(Product.builder()
                    .name("Wireless Headphones")
                    .description("Noise-cancelling Bluetooth headphones with 30h battery")
                    .price(new BigDecimal("99.99"))
                    .stockQuantity(50)
                    .imageUrl("https://example.com/images/headphones.jpg")
                    .category(electronics)
                    .build());

            productRepository.save(Product.builder()
                    .name("Mechanical Keyboard")
                    .description("Compact TKL mechanical keyboard with RGB backlight")
                    .price(new BigDecimal("149.99"))
                    .stockQuantity(30)
                    .imageUrl("https://example.com/images/keyboard.jpg")
                    .category(electronics)
                    .build());

            productRepository.save(Product.builder()
                    .name("Classic White T-Shirt")
                    .description("100% cotton unisex t-shirt, available in all sizes")
                    .price(new BigDecimal("19.99"))
                    .stockQuantity(200)
                    .imageUrl("https://example.com/images/tshirt.jpg")
                    .category(clothing)
                    .build());

            log.info("Sample categories and products seeded");
        }
    }
}
