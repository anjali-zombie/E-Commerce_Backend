package org.panda.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.panda.ecommerce.dto.request.ProductRequest;
import org.panda.ecommerce.dto.response.PagedResponse;
import org.panda.ecommerce.dto.response.ProductResponse;
import org.panda.ecommerce.entity.Category;
import org.panda.ecommerce.entity.Product;
import org.panda.ecommerce.exception.ResourceNotFoundException;
import org.panda.ecommerce.repository.CategoryRepository;
import org.panda.ecommerce.repository.ProductRepository;
import org.panda.ecommerce.service.impl.ProductServiceImpl;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_withoutCategory_shouldReturnProductResponse() {
        ProductRequest request = new ProductRequest();
        request.setName("Widget");
        request.setPrice(new BigDecimal("9.99"));
        request.setStockQuantity(100);

        Product saved = Product.builder().id(1L).name("Widget")
                .price(new BigDecimal("9.99")).stockQuantity(100).active(true).build();

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getName()).isEqualTo("Widget");
        assertThat(response.getPrice()).isEqualByComparingTo("9.99");
    }

    @Test
    void createProduct_withCategory_shouldSetCategory() {
        ProductRequest request = new ProductRequest();
        request.setName("Gadget");
        request.setPrice(new BigDecimal("29.99"));
        request.setStockQuantity(50);
        request.setCategoryId(1L);

        Category category = Category.builder().id(1L).name("Electronics").build();
        Product saved = Product.builder().id(2L).name("Gadget")
                .price(new BigDecimal("29.99")).stockQuantity(50).category(category).active(true).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getCategory()).isNotNull();
        assertThat(response.getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    void getProductById_activeProduct_shouldReturnProduct() {
        Product product = Product.builder().id(1L).name("Widget")
                .price(new BigDecimal("9.99")).stockQuantity(10).active(true).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_inactiveProduct_shouldThrowNotFound() {
        Product product = Product.builder().id(1L).name("Deleted").active(false).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllProducts_shouldReturnPagedResponse() {
        Product p1 = Product.builder().id(1L).name("A").price(BigDecimal.ONE).stockQuantity(5).active(true).build();
        Product p2 = Product.builder().id(2L).name("B").price(BigDecimal.TEN).stockQuantity(3).active(true).build();

        when(productRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p1, p2)));

        PagedResponse<ProductResponse> result = productService.getAllProducts(0, 10, "createdAt", "desc");

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void deleteProduct_existingProduct_shouldSoftDelete() {
        Product product = Product.builder().id(1L).name("Widget").active(true).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.deleteProduct(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_notFound_shouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
