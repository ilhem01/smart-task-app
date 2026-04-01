package com.smarttask.auth.controller;

import com.smarttask.auth.entity.User;
import com.smarttask.auth.repository.UserRepository;
import com.smarttask.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register users and obtain JWT access tokens")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates an account with ROLE_USER. Returns 409 if the username is already taken.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created (plain text: User registered successfully)"),
            @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Validates credentials and returns a JWT for use with the API Gateway and Task Service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT issued",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return userRepository.findByUsername(request.username())
                .map(user -> {
                    boolean validPassword = passwordEncoder.matches(request.password(), user.getPassword());
                    if (!validPassword) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
                    }

                    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                    return ResponseEntity.ok(new AuthResponse(token));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials"));
    }

    @Schema(name = "AuthRequest", description = "Username and password")
    public record AuthRequest(
            @Schema(example = "alice") String username,
            @Schema(example = "password123") String password) {
    }

    @Schema(name = "AuthResponse", description = "JWT bearer token")
    public record AuthResponse(
            @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token) {
    }
}
