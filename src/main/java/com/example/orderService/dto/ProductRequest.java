package com.example.orderService.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 50)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.1", message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "stock is required")
        @Min(value = 0, message = "stock can't be negative")
        Integer stock
) {}