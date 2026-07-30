package com.bmart.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
}
