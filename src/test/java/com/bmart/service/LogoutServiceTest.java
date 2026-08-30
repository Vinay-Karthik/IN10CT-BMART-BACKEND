package com.bmart.service;

import com.bmart.entity.JwtToken;
import com.bmart.repository.JwtTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private JwtTokenRepository jwtTokenRepository;

    @InjectMocks
    private LogoutService logoutService;

    private Long sampleUserId;
    private JwtToken sampleToken;

    @BeforeEach
    void setUp() {
        sampleUserId = 101L;
        sampleToken = JwtToken.builder()
                .id(1L)
                .userId(sampleUserId)
                .token("sample.jwt.token.string")
                .build();
    }

    @Test
    @DisplayName("1. Successful Logout - Deletes stored token when found")
    void testLogoutSuccess() {
        // Arrange
        when(jwtTokenRepository.findByUserId(sampleUserId)).thenReturn(Optional.of(sampleToken));
        doNothing().when(jwtTokenRepository).deleteByUserId(sampleUserId);

        // Act & Assert
        assertDoesNotThrow(() -> logoutService.logout(sampleUserId));

        // Verify Repository Invocations
        verify(jwtTokenRepository, times(1)).findByUserId(sampleUserId);
        verify(jwtTokenRepository, times(1)).deleteByUserId(sampleUserId);
    }

    @Test
    @DisplayName("2. Logout with No Existing Token - Exits silently without throwing exception (Idempotent)")
    void testLogoutNoExistingToken() {
        // Arrange: No token stored for this user ID
        when(jwtTokenRepository.findByUserId(sampleUserId)).thenReturn(Optional.empty());

        // Act & Assert: Should execute silently without throwing
        assertDoesNotThrow(() -> logoutService.logout(sampleUserId));

        // Verify deleteByUserId is NOT invoked when token is missing
        verify(jwtTokenRepository, times(1)).findByUserId(sampleUserId);
        verify(jwtTokenRepository, never()).deleteByUserId(anyLong());
    }

    @Test
    @DisplayName("3. Logout Triggering Unexpected Exception - Exception handling flow")
    void testLogoutUnexpectedException() {
        // Arrange: Repository throws database connection error
        when(jwtTokenRepository.findByUserId(sampleUserId)).thenThrow(new RuntimeException("Database error during lookup"));

        // Act & Assert: Exception propagates so controller catch block returns 500
        RuntimeException exception = assertThrows(RuntimeException.class, () -> logoutService.logout(sampleUserId));
        assertEquals("Database error during lookup", exception.getMessage());

        verify(jwtTokenRepository, times(1)).findByUserId(sampleUserId);
    }
}
