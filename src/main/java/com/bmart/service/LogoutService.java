package com.bmart.service;

import com.bmart.entity.JwtToken;
import com.bmart.repository.JwtTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {

    private final JwtTokenRepository jwtTokenRepository;

    @Transactional
    public void logout(Long userId) {
        if (userId == null) {
            log.warn("Logout attempt with null userId - exiting silently.");
            return;
        }

        Optional<JwtToken> existingToken = jwtTokenRepository.findByUserId(userId);
        if (existingToken.isPresent()) {
            jwtTokenRepository.deleteByUserId(userId);
            log.info("Successfully deleted JWT token for userId: {}", userId);
        } else {
            log.info("No active JWT token found for userId: {} - idempotent logout completed.", userId);
        }
    }
}
