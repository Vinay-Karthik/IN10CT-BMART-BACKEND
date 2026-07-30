package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.User;
import com.bmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", userService.getProfile(userDetails.getUsername())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userService.updateProfile(userDetails.getUsername(), body)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        userService.changePassword(userDetails.getUsername(), body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
