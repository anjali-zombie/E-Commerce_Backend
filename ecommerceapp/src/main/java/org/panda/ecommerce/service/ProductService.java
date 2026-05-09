package org.panda.ecommerce.service;

import org.panda.ecommerce.dto.request.ProductRequest;
import org.panda.ecommerce.dto.response.PagedResponse;
import org.panda.ecommerce.dto.response.ProductResponse;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);

    PagedResponse<ProductResponse> searchProducts(String query, int page, int size);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
