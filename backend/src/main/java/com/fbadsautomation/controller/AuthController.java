package com.fbadsautomation.controller;

import com.fbadsautomation.model.User;
import com.fbadsautomation.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://linhnh.site", "http://localhost:8081"})
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
}

