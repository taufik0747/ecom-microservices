package com.ecommerce.product;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductTestService {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product product;
    private Product savedProduct;

    @BeforeEach
    void setUp() {
        // Arrange - Set up test data
        productRequest = createProductRequest();
        product = createProduct();
        savedProduct = createSavedProduct();
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProductSuccessfully() {
            // Arrange
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // Act
            ProductResponse result = productService.createProduct(productRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedProduct.getId());
            assertThat(result.getName()).isEqualTo(productRequest.getName());
            assertThat(result.getDescription()).isEqualTo(productRequest.getDescription());
            assertThat(result.getPrice()).isEqualTo(productRequest.getPrice());
            assertThat(result.getStockQuantity()).isEqualTo(productRequest.getStockQuantity());
            assertThat(result.getCategory()).isEqualTo(productRequest.getCategory());
            assertThat(result.getImageUrl()).isEqualTo(productRequest.getImageUrl());
            assertThat(result.getActive()).isTrue();

            // Verify interactions
            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("Should handle null product request gracefully")
        void shouldHandleNullProductRequest() {
            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                    productService.createProduct(null)
            );

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should create product with minimum required fields")
        void shouldCreateProductWithMinimumFields() {
            // Arrange
            ProductRequest minimalRequest = new ProductRequest();
            minimalRequest.setName("Minimal Product");
            minimalRequest.setPrice(BigDecimal.valueOf(10.0));

            Product minimalProduct = new Product();
            minimalProduct.setId(1L);
            minimalProduct.setName("Minimal Product");
            minimalProduct.setPrice(BigDecimal.valueOf(10.0));
            minimalProduct.setActive(true);

            when(productRepository.save(any(Product.class))).thenReturn(minimalProduct);

            // Act
            ProductResponse result = productService.createProduct(minimalRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Minimal Product");
            assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(10.0));
            assertThat(result.getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update existing product successfully")
        void shouldUpdateExistingProductSuccessfully() {
            // Arrange
            Long productId = 1L;
            Product existingProduct = createProduct();
            existingProduct.setId(productId);

            ProductRequest updateRequest = createProductRequest();
            updateRequest.setName("Updated Product");
            updateRequest.setPrice(BigDecimal.valueOf(299.99));

            Product updatedProduct = createSavedProduct();
            updatedProduct.setName("Updated Product");
            updatedProduct.setPrice(BigDecimal.valueOf(299.99));

            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

            // Act
            Optional<ProductResponse> result = productService.updateProduct(productId, updateRequest);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Updated Product");
            assertThat(result.get().getPrice()).isEqualTo(BigDecimal.valueOf(299.99));

            verify(productRepository, times(1)).findById(productId);
            verify(productRepository, times(1)).save(existingProduct);
        }

        @Test
        @DisplayName("Should return empty when product not found")
        void shouldReturnEmptyWhenProductNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act
            Optional<ProductResponse> result = productService.updateProduct(nonExistentId, productRequest);

            // Assert
            assertThat(result).isEmpty();

            verify(productRepository, times(1)).findById(nonExistentId);
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should handle null update request")
        void shouldHandleNullUpdateRequest() {
            // Arrange
            Long productId = 1L;
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                    productService.updateProduct(productId, null)
            );
        }
    }

    @Nested
    @DisplayName("Get All Products Tests")
    class GetAllProductsTests {

        @Test
        @DisplayName("Should return all active products")
        void shouldReturnAllActiveProducts() {
            // Arrange
            List<Product> activeProducts = Arrays.asList(
                    createSavedProduct(),
                    createSecondProduct()
            );
            when(productRepository.findByActiveTrue()).thenReturn(activeProducts);

            // Act
            List<ProductResponse> result = productService.getAllProducts();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Test Product");
            assertThat(result.get(1).getName()).isEqualTo("Second Product");
            assertThat(result).allMatch(ProductResponse::getActive);

            verify(productRepository, times(1)).findByActiveTrue();
        }

        @Test
        @DisplayName("Should return empty list when no active products")
        void shouldReturnEmptyListWhenNoActiveProducts() {
            // Arrange
            when(productRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

            // Act
            List<ProductResponse> result = productService.getAllProducts();

            // Assert
            assertThat(result).isEmpty();

            verify(productRepository, times(1)).findByActiveTrue();
        }

        @Test
        @DisplayName("Should not return inactive products")
        void shouldNotReturnInactiveProducts() {
            // Arrange
            Product activeProduct = createSavedProduct();
            when(productRepository.findByActiveTrue()).thenReturn(List.of(activeProduct));

            // Act
            List<ProductResponse> result = productService.getAllProducts();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should soft delete existing product")
        void shouldSoftDeleteExistingProduct() {
            // Arrange
            Long productId = 1L;
            Product existingProduct = createSavedProduct();
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

            // Act
            boolean result = productService.deleteProduct(productId);

            // Assert
            assertThat(result).isTrue();
            assertThat(existingProduct.getActive()).isFalse();

            verify(productRepository, times(1)).findById(productId);
            verify(productRepository, times(1)).save(existingProduct);
        }

        @Test
        @DisplayName("Should return false when product not found")
        void shouldReturnFalseWhenProductNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act
            boolean result = productService.deleteProduct(nonExistentId);

            // Assert
            assertThat(result).isFalse();

            verify(productRepository, times(1)).findById(nonExistentId);
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should handle null product ID")
        void shouldHandleNullProductId() {
            // Act
            boolean result = productService.deleteProduct(null);

            // Assert
            assertThat(result).isFalse();

            verify(productRepository, times(1)).findById(null);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("Search Products Tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should return products matching search keyword")
        void shouldReturnProductsMatchingSearchKeyword() {
            // Arrange
            String keyword = "laptop";
            List<Product> searchResults = Arrays.asList(
                    createSavedProduct(),
                    createSecondProduct()
            );
            when(productRepository.searchProducts(keyword)).thenReturn(searchResults);

            // Act
            List<ProductResponse> result = productService.searchProducts(keyword);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Test Product");
            assertThat(result.get(1).getName()).isEqualTo("Second Product");

            verify(productRepository, times(1)).searchProducts(keyword);
        }

        @Test
        @DisplayName("Should return empty list when no products match keyword")
        void shouldReturnEmptyListWhenNoProductsMatch() {
            // Arrange
            String keyword = "nonexistent";
            when(productRepository.searchProducts(keyword)).thenReturn(Collections.emptyList());

            // Act
            List<ProductResponse> result = productService.searchProducts(keyword);

            // Assert
            assertThat(result).isEmpty();

            verify(productRepository, times(1)).searchProducts(keyword);
        }

        @Test
        @DisplayName("Should handle empty search keyword")
        void shouldHandleEmptySearchKeyword() {
            // Arrange
            String emptyKeyword = "";
            when(productRepository.searchProducts(emptyKeyword)).thenReturn(Collections.emptyList());

            // Act
            List<ProductResponse> result = productService.searchProducts(emptyKeyword);

            // Assert
            assertThat(result).isEmpty();

            verify(productRepository, times(1)).searchProducts(emptyKeyword);
        }

        @Test
        @DisplayName("Should handle null search keyword")
        void shouldHandleNullSearchKeyword() {
            // Arrange
            when(productRepository.searchProducts(null)).thenReturn(Collections.emptyList());

            // Act
            List<ProductResponse> result = productService.searchProducts(null);

            // Assert
            assertThat(result).isEmpty();

            verify(productRepository, times(1)).searchProducts(null);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete product lifecycle")
        void shouldHandleCompleteProductLifecycle() {
            // Arrange
            Long productId = 1L;
            Product existingProduct = createSavedProduct();

            // Mock create
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // Mock update
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(existingProduct)).thenReturn(existingProduct);

            // Act - Create
            ProductResponse created = productService.createProduct(productRequest);

            // Act - Update
            ProductRequest updateRequest = createProductRequest();
            updateRequest.setName("Updated Name");
            Optional<ProductResponse> updated = productService.updateProduct(productId, updateRequest);

            // Act - Delete
            boolean deleted = productService.deleteProduct(productId);

            // Assert
            assertThat(created).isNotNull();
            assertThat(updated).isPresent();
            assertThat(deleted).isTrue();
            assertThat(existingProduct.getActive()).isFalse();
        }
    }

    // Helper methods for creating test data
    private ProductRequest createProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(BigDecimal.valueOf(199.99));
        request.setStockQuantity(100);
        request.setCategory("Electronics");
        request.setImageUrl("https://example.com/image.jpg");
        return request;
    }

    private Product createProduct() {
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(BigDecimal.valueOf(199.99));
        product.setStockQuantity(100);
        product.setCategory("Electronics");
        product.setImageUrl("https://example.com/image.jpg");
        product.setActive(true);
        return product;
    }

    private Product createSavedProduct() {
        Product product = createProduct();
        product.setId(1L);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    private Product createSecondProduct() {
        Product product = new Product();
        product.setId(2L);
        product.setName("Second Product");
        product.setDescription("Second Description");
        product.setPrice(BigDecimal.valueOf(299.99));
        product.setStockQuantity(50);
        product.setCategory("Books");
        product.setImageUrl("https://example.com/image2.jpg");
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}