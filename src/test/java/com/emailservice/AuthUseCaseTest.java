package com.emailservice;

import com.emailservice.application.dto.LoginRequest;
import com.emailservice.application.dto.RegisterRequest;
import com.emailservice.application.port.TokenService;
import com.emailservice.application.usecase.AuthUseCase;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthUseCase authUseCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .enabled(true)
                .build();
    }

    @Test
    void register_Success() {
        var request = new RegisterRequest("newuser", "new@example.com", "plain-password");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-token");

        var result = authUseCase.register(request);

        assertNotNull(result);
        assertEquals("jwt-token", result.token());
        assertEquals("newuser", result.username());
        assertEquals("new@example.com", result.email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("encoded-password", saved.getPassword());
        assertTrue(saved.isEnabled());
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        var request = new RegisterRequest("testuser", "new@example.com", "plain-password");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authUseCase.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        var request = new RegisterRequest("newuser", "test@example.com", "plain-password");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authUseCase.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        var request = new LoginRequest("testuser", "plain-password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tokenService.generateToken(testUser)).thenReturn("jwt-token");

        var result = authUseCase.login(request);

        assertNotNull(result);
        assertEquals("jwt-token", result.token());
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_BadCredentials_Propagates() {
        var request = new LoginRequest("testuser", "wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authUseCase.login(request));
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        var request = new LoginRequest("ghost", "plain-password");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authUseCase.login(request));
    }
}
