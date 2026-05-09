package org.panda.ecommerce.repository;

import org.junit.jupiter.api.Test;
import org.panda.ecommerce.entity.Category;
import org.panda.ecommerce.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByActiveTrue_shouldReturnOnlyActiveProducts() {
        productRepository.save(Product.builder().name("Active Product")
                .price(new BigDecimal("10.00")).stockQuantity(5).active(true).build());
        productRepository.save(Product.builder().name("Inactive Product")
                .price(new BigDecimal("20.00")).stockQuantity(3).active(false).build());

        Page<Product> results = productRepository.findByActiveTrue(PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Active Product");
    }

    @Test
    void findByNameContainingIgnoreCaseAndActiveTrue_shouldPerformCaseInsensitiveSearch() {
        productRepository.save(Product.builder().name("Bluetooth Speaker")
                .price(new BigDecimal("49.99")).stockQuantity(10).active(true).build());
        productRepository.save(Product.builder().name("Wireless Mouse")
                .price(new BigDecimal("29.99")).stockQuantity(8).active(true).build());

        Page<Product> results = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(
                "bluetooth", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Bluetooth Speaker");
    }

    @Test
    void findByCategoryIdAndActiveTrue_shouldFilterByCategory() {
        Category electronics = categoryRepository.save(
                Category.builder().name("Electronics").build());
        Category clothing = categoryRepository.save(
                Category.builder().name("Clothing").build());

        productRepository.save(Product.builder().name("Phone")
                .price(new BigDecimal("699.99")).stockQuantity(5).active(true).category(electronics).build());
        productRepository.save(Product.builder().name("T-Shirt")
                .price(new BigDecimal("19.99")).stockQuantity(50).active(true).category(clothing).build());

        Page<Product> results = productRepository.findByCategoryIdAndActiveTrue(
                electronics.getId(), PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Phone");
    }

    @Test
    void findByCategoryIdAndActiveTrue_shouldNotReturnInactiveProducts() {
        Category electronics = categoryRepository.save(
                Category.builder().name("Gadgets").build());

        productRepository.save(Product.builder().name("Active Gadget")
                .price(new BigDecimal("99.99")).stockQuantity(5).active(true).category(electronics).build());
        productRepository.save(Product.builder().name("Discontinued Gadget")
                .price(new BigDecimal("49.99")).stockQuantity(0).active(false).category(electronics).build());

        Page<Product> results = productRepository.findByCategoryIdAndActiveTrue(
                electronics.getId(), PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
    }
}
