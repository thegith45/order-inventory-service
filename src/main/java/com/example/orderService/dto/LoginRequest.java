package com.example.orderService.dto;

public record LoginRequest(
        String email,
        String password
) {}