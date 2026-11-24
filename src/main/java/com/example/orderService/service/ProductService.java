package com.example.orderService.service;

import com.example.orderService.dto.ProductRequest;
import com.example.orderService.dto.ProductResponse;
import com.example.orderService.entity.Product;
import com.example.orderService.repository.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Create new product
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );
        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
    }

    // Update product
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductEntity(id);

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.from(updatedProduct);
    }

    // Soft deleting product
    public void deleteProduct(Long id) {
        Product product = getProductEntity(id);
        product.setDeleted(true);
        productRepository.save(product);
    }

    // Get one product
    public ProductResponse getProduct(Long id) {
        Product product = getProductEntity(id);
        return ProductResponse.from(product);
    }

    // Get products with search and pagination
    public Page<ProductResponse> getProducts(String search, Pageable pageable) {
        Page<Product> products;
        if (search == null || search.isBlank()) {
            products = productRepository.findByDeletedFalse(pageable);
        }
        else {
            products = productRepository
                    .findByNameContainingIgnoreCaseAndDeletedFalse(search, pageable);
        }
        return products.map(ProductResponse::from);
    }

    // helper fn.
    private Product getProductEntity(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Product not found"));
    }
}
