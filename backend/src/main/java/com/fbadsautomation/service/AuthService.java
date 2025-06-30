package com.fbadsautomation.service;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.integration.facebook.FacebookApiClient;
import com.fbadsautomation.integration.facebook.FacebookProperties;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.UserRepository;
import com.fbadsautomation.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final FacebookApiClient facebookApiClient;
    private final FacebookProperties facebookProperties;
    private final JwtTokenProvider jwtTokenProvider;

    // Store state for CSRF protection
    private final Map<String, String> stateStore = new ConcurrentHashMap<>( );

    // Store temporary auth tokens for success redirect
    private final Map<String, Map<String, String>> authTokenStore = new ConcurrentHashMap<>();

    /**
     * Get Facebook authorization URL
     * @return The authorization URL
     */
    public String getFacebookAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, "pending");
        return String.format(
            "https://www.facebook.com/v%s/dialog/oauth?client_id=%s&redirect_uri=%s&state=%s&scope=%s",
            facebookProperties.getApiVersion( ),
            facebookProperties.getAppId(),
            facebookProperties.getRedirectUri(),
            state,
            facebookProperties.getScope()
        );
    }

    /**
     * Process Facebook callback
     * @param code The authorization code
     * @param state The state parameter
     * @return Map containing token and user info
     */
    @Transactional
    public Map<String, String> processFacebookCallback(String code, String state) {
        // Verify state to prevent CSRF
        if (!stateStore.containsKey(state) || !"pending".equals(stateStore.get(state))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid state parameter");
        }

        try {
            // Exchange code for access token
            String accessToken = facebookApiClient.getAccessToken(code, facebookProperties.getRedirectUri());
            log.info("Facebook Access Token: {}", accessToken);

            // Get user info from Facebook
            Map<String, Object> userInfo = facebookApiClient.getUserInfo(accessToken);
            log.info("Facebook User Info: {}", userInfo);
            String fbUserId = (String) userInfo.get("id");
            String name = (String) userInfo.get("name");
            String email = (String) userInfo.get("email");

            // Find or create user
            User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    // newUser.setFbUserId(fbUserId); // Removed as per new new requirements
                    // newUser.setFbAccessToken(accessToken); // Removed as per new new requirements
                    return userRepository.save(newUser);
                });

            // Update token if user exists
            if (user.getId() != null) {
                user.setName(name);
                user.setEmail(email);
                // user.setFbAccessToken(accessToken); // Removed as per new new requirements
                userRepository.save(user);
            }

            // Generate JWT token for our application
            String jwtToken = generateJwtToken(user);

            // Clean up state
            stateStore.remove(state);

            // Store token info for success redirect
            String sessionId = UUID.randomUUID().toString();
            Map<String, String> tokenInfo = new HashMap<>();
            tokenInfo.put("token", jwtToken);
            tokenInfo.put("userId", user.getId().toString());
            tokenInfo.put("name", user.getName());
            tokenInfo.put("email", user.getEmail());
            authTokenStore.put(sessionId, tokenInfo);

            // Add session ID to response for success endpoint
            Map<String, String> response = new HashMap<>(tokenInfo);
            response.put("sessionId", sessionId);

            return response;
        } catch (Exception e) {
            log.error("Error processing Facebook callback: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process Facebook callback: " + e.getMessage());
        }
    }

    /**
     * Get stored auth token by session ID
     * @param sessionId The session ID
     * @return Map containing token and user info
     */
    public Map<String, String> getStoredAuthToken(String sessionId) {
        if (sessionId == null || !authTokenStore.containsKey(sessionId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired session");
        }

        // Get token info and remove from store (one-time use)
        Map<String, String> tokenInfo = authTokenStore.remove(sessionId);
        return tokenInfo;
    }

    /**
     * Generate token for current authenticated user
     * @return Map containing token and user info
     */
    public Map<String, String> generateTokenForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String userId = authentication.getName();
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Generate JWT token for our application
        String jwtToken = jwtTokenProvider.generateToken(user.getId());

        // Return token and user info
        Map<String, String> response = new HashMap<>();
        response.put("token", jwtToken);
        response.put("userId", user.getId().toString());
        response.put("name", user.getName());
        response.put("email", user.getEmail());

        return response;
    }

    /**
     * Get current authenticated user
     * @return The current user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String userId = authentication.getName();
        log.info("Attempting to retrieve user with ID: {}", userId);
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> {
                log.error("User not found with ID: {}", userId);
                return new ApiException(HttpStatus.NOT_FOUND, "User not found");
            });
        log.info("Successfully retrieved user: {} with email: {}", user.getId(), user.getEmail());
        return user;
    }

    /**
     * Logout current user
     */
    public void logout() {
        // In a real implementation, this would invalidate the JWT token
        // For now, just log the action
        log.info("User logged out");
    }

    /**
     * Process OAuth2 user from Spring Security
     * @param oauth2User The OAuth2 user from Spring Security
     * @return JWT token for our application
     */
    @Transactional
    public String processOAuth2User(org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
        try {
            // Extract user information from OAuth2User
            String fbUserId = oauth2User.getAttribute("id");
            String name = oauth2User.getAttribute("name");
            String email = oauth2User.getAttribute("email");
            
            log.info("Processing OAuth2 user: id={}, name={}, email={}", fbUserId, name, email);
            
            if (email == null) { // Changed from fbUserId to email as primary identifier
                throw new ApiException(HttpStatus.BAD_REQUEST, "Email not found in OAuth2 response");
            }

            // Find or create user
            User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    // newUser.setFbUserId(fbUserId); // Removed as per new new requirements
                    return userRepository.save(newUser);
                });

            // Update user info if exists
            if (user.getId() != null) {
                user.setName(name);
                user.setEmail(email);
                userRepository.save(user);
            }

            // Generate JWT token for our application
            String jwtToken = generateJwtToken(user);
            
            log.info("Successfully processed OAuth2 user {} and generated token", email);
            return jwtToken;
            
        } catch (Exception e) {
            log.error("Error processing OAuth2 user: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process OAuth2 user: " + e.getMessage());
        }
    }

    /**
     * Generate JWT token for user
     * @param user The user
     * @return The JWT token
     */
    private String generateJwtToken(User user) {
        // Use JwtTokenProvider to generate a proper JWT token
        return jwtTokenProvider.generateToken(user.getId());
    }
    
    /**
     * Get current user from authentication
     * @param authentication The authentication object
     * @return Current user
     */
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String userId = authentication.getName();
        log.info("Attempting to retrieve user with ID: {}", userId);
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> {
                log.error("User not found with ID: {}", userId);
                return new ApiException(HttpStatus.NOT_FOUND, "User not found");
            });
        log.info("Successfully retrieved user: {} with email: {}", user.getId(), user.getEmail());
        return user;
    }
}