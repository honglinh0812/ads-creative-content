package com.fbadsautomation.controller;

import com.fbadsautomation.model.User;
import com.fbadsautomation.service.AuthService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://linhnh.site", "http://localhost:8081", "http://localhost:3000"})

public class AuthController {

    private final AuthService authService;
    /*
    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser() {
        log.info("Getting current user");
        return ResponseEntity.ok(authService.getCurrentUser());
    }
    */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        log.info("Getting current user");
        User user = authService.getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("name", user.getName());
        return ResponseEntity.ok(map);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Processing logout");
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String email = req.get("email");
        String password = req.get("password");
        authService.registerUser(username, email, password);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login-app")
    public ResponseEntity<?> loginApp(@RequestBody Map<String, String> req) {
        String usernameOrEmail = req.get("usernameOrEmail");
        String password = req.get("password");
        String token = authService.loginWithUsernamePassword(usernameOrEmail, password);
        Map<String, String> resp = new HashMap<>();
        resp.put("token", token);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        authService.forgotPassword(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        String token = req.get("token");
        String newPassword = req.get("newPassword");
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok().build();
    }
}
