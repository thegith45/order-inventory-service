package com.example.orderService.controller;

import com.example.orderService.dto.AuthResponse;
import com.example.orderService.dto.LoginRequest;
import com.example.orderService.dto.RegisterRequest;
import com.example.orderService.dto.UserResponse;
import com.example.orderService.entity.User;
import com.example.orderService.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody RegisterRequest request){
        User user = userService.register(request);
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req){
        return userService.login(req);
    }
}
