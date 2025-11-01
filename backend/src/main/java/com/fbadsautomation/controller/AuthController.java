package com.fbadsautomation.controller;

import com.fbadsautomation.model.User;
import com.fbadsautomation.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"https://linhnh.site", "https://api.linhnh.site"})
@Tag(name = "Authentication", description = "API endpoints for user authentication and account management")

public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    /*
    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser() {
        log.info("Getting current user");
        return ResponseEntity.ok(authService.getCurrentUser());
    }
    */
    @Operation(summary = "Get current user", description = "Retrieve information about the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        log.info("Getting current user");
        User user = authService.getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("name", user.getName());
        map.put("username", user.getUsername());
        return ResponseEntity.ok(map);
    }

    @Operation(summary = "Logout user", description = "Logout the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Processing logout");
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register new user", description = "Create a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Conflict - User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
        @Parameter(description = "Registration data including username, email, and password")
        @RequestBody Map<String, String> req) {
        String username = req.get("username");
        String email = req.get("email");
        String password = req.get("password");
        authService.registerUser(username, email, password);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Login user", description = "Authenticate user with username/email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful, returns JWT token"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials")
    })
    @PostMapping("/login-app")
    public ResponseEntity<?> loginApp(
        @Parameter(description = "Login credentials with usernameOrEmail and password")
        @RequestBody Map<String, String> req) {
        String usernameOrEmail = req.get("usernameOrEmail");
        String password = req.get("password");
        String token = authService.loginWithUsernamePassword(usernameOrEmail, password);
        Map<String, String> resp = new HashMap<>();
        resp.put("token", token);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Forgot password", description = "Send password reset email to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
        @Parameter(description = "Email address for password reset")
        @RequestBody Map<String, String> req) {
        String email = req.get("email");
        authService.forgotPassword(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset password", description = "Reset user password using reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired reset token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
        @Parameter(description = "Reset token and new password")
        @RequestBody Map<String, String> req) {
        String token = req.get("token");
        String newPassword = req.get("newPassword");
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get user profile", description = "Retrieve detailed profile information of the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        log.info("Getting user profile");
        User user = authService.getCurrentUser();
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("name", user.getName());
        profile.put("username", user.getUsername());
        profile.put("firstName", user.getName() != null ? user.getName().split(" ")[0] : "");
        profile.put("lastName", user.getName() != null && user.getName().contains(" ") ? user.getName().substring(user.getName().indexOf(" ") + 1) : "");
        profile.put("phoneNumber", "");
        profile.put("company", "");
        profile.put("jobTitle", "");
        profile.put("language", "en");
        profile.put("timezone", "Asia/Ho_Chi_Minh");
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update user profile", description = "Update profile information of the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid profile data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
        @Parameter(description = "Profile data to update")
        @RequestBody Map<String, Object> profileData) {
        log.info("Updating user profile");
        // For now, just return success - can be implemented later with proper user fields
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change password", description = "Change password for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid password data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
        @Parameter(description = "Current and new password data")
        @RequestBody Map<String, String> passwordData) {
        log.info("Changing user password");
        // For now, just return success - can be implemented later
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete user account", description = "Permanently delete the current user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        log.info("Deleting user account");
        // For now, just return success - can be implemented later
        return ResponseEntity.ok().build();
    }
}
