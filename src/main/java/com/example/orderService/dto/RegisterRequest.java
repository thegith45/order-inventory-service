package com.example.orderService.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {}