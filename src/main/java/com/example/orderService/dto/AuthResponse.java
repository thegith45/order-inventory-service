package com.example.orderService.dto;

public record AuthResponse(
        String token,
        String tokenType
) {}