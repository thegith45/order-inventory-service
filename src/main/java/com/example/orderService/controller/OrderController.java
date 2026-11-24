package com.example.orderService.controller;

import com.example.orderService.dto.OrderResponse;
import com.example.orderService.dto.PlaceOrderRequest;
import com.example.orderService.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
            @Valid @RequestBody PlaceOrderRequest request, Authentication auth){
        return orderService.placeOrder(
                auth.getName(),// this is email
                request
        );
    }

    @GetMapping
    public Page<OrderResponse> getOrders(Authentication auth
            ,@PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable){
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(au->au
                        .getAuthority().equals("ROLE_ADMIN"));
        return orderService.getOrders(auth.getName(),isAdmin,pageable);
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId,
                                     Authentication auth){
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(au->au
                        .getAuthority().equals("ROLE_ADMIN"));
        return orderService.cancelOrder(orderId,auth.getName(),isAdmin);
    }
}