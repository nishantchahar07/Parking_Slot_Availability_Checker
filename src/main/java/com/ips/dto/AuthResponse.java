package com.ips.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private String role;
    private String userId;
}
