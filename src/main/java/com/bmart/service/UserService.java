package com.bmart.service;

import com.bmart.entity.User;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = "users", key = "#identifier")
    public User getProfile(String identifier) {
        return userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updateProfile(String identifier, Map<String, String> data) {
        User user = getProfile(identifier);

        if (data.containsKey("fullName")) user.setFullName(data.get("fullName"));
        if (data.containsKey("phoneNumber")) user.setPhoneNumber(data.get("phoneNumber"));
        if (data.containsKey("address")) user.setAddress(data.get("address"));
        if (data.containsKey("city")) user.setCity(data.get("city"));
        if (data.containsKey("state")) user.setState(data.get("state"));
        if (data.containsKey("pincode")) user.setPincode(data.get("pincode"));

        return userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void changePassword(String identifier, String oldPassword, String newPassword) {
        User user = getProfile(identifier);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
