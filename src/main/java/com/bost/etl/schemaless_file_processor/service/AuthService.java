package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.AuthResponse;
import com.bost.etl.schemaless_file_processor.dto.LoginRequest;
import com.bost.etl.schemaless_file_processor.dto.RegisterRequest;
import com.bost.etl.schemaless_file_processor.entity.User;
import com.bost.etl.schemaless_file_processor.repository.UserRepository;
import com.bost.etl.schemaless_file_processor.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        validateRegistration(request);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(request.getRoles())
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = extractUserFromAuthentication(authentication);
        String token = jwtUtil.generateToken(user);

        log.info("User logged in successfully: {} (ID: {})", user.getUsername(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    private void validateRegistration(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
    }

    private User extractUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication failed");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        if (principal instanceof UserDetails) {
            // If the principal is a different UserDetails implementation, fetch from database
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadCredentialsException("User not found: " + username));
        }

        throw new BadCredentialsException("Invalid authentication principal");
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }
}