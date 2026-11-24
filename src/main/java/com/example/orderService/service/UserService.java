package com.example.orderService.service;

import com.example.orderService.dto.AuthResponse;
import com.example.orderService.dto.LoginRequest;
import com.example.orderService.dto.RegisterRequest;
import com.example.orderService.entity.User;
import com.example.orderService.entity.UserRole;
import com.example.orderService.repository.UserRepository;
import org.springframework.http.HttpStatus;
//import org.springframework.security.config.authentication.PasswordEncoderParser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService
    ){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {

        if(userRepository.existsByEmail(request.email())){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        String hashedPwd = passwordEncoder.encode(request.password());

        User user = new User(request.name(),
                    request.email(),
                    hashedPwd,
                    UserRole.CUSTOMER
        );

        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req){
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if(!passwordEncoder.matches(
                req.password(),
                user.getPassword()
        )){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
        String token = jwtService.generateToken(user);

        return new AuthResponse(token,"Bearer");
    }
}
