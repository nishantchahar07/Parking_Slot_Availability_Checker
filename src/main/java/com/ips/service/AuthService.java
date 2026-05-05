package com.ips.service;

import com.ips.dto.*;
import com.ips.model.User;
import com.ips.model.enums.Role;
import com.ips.repository.UserRepository;
import com.ips.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.valueOf(req.getRole().toUpperCase()))
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId(), user.getName());
        return AuthResponse.builder()
                .token(token).name(user.getName()).email(user.getEmail())
                .role(user.getRole().name()).userId(user.getId()).build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId(), user.getName());
        return AuthResponse.builder()
                .token(token).name(user.getName()).email(user.getEmail())
                .role(user.getRole().name()).userId(user.getId()).build();
    }
}
