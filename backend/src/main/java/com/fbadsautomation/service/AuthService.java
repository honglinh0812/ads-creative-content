package com.fbadsautomation.service;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.integration.facebook.FacebookApiClient;
import com.fbadsautomation.integration.facebook.FacebookProperties;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.UserRepository;
import com.fbadsautomation.security.JwtTokenProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FacebookApiClient facebookApiClient;
    private final FacebookProperties facebookProperties;
    private final JwtTokenProvider jwtTokenProvider;

    // Store state for CSRF protection
    private final Map<String, String> stateStore = new ConcurrentHashMap<>(); // Store temporary auth tokens for success redirect
    private final Map<String, Map<String, String>> authTokenStore = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, String> resetTokenStore = new ConcurrentHashMap<>(); // token -> username/email
    // Nếu có JavaMailSender thì inject vào đây
    //@Autowired
    //private JavaMailSender mailSender;
    @Value("${APP_RESET_PASSWORD_BASE_URL:https://linhnh.site/reset-password}")
    private String resetPasswordBaseUrl;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    public AuthService(UserRepository userRepository, FacebookApiClient facebookApiClient, 
                      FacebookProperties facebookProperties, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.facebookApiClient = facebookApiClient;
        this.facebookProperties = facebookProperties;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // Đăng ký tài khoản mới
    public void registerUser(String username, String email, String password) {
        username = username.trim();
        email = email.trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    // Đăng nhập bằng username/password hoặc email/password
    public String loginWithUsernamePassword(String usernameOrEmail, String password) {
        String input = usernameOrEmail.trim();
        log.info("Login attempt with: '{}'", input);

        Optional<User> userByUsername = userRepository.findByUsernameIgnoreCase(input);
        if (userByUsername.isPresent()) {
            log.info("Found user by username: {}", userByUsername.get().getUsername());
        } else {
            log.info("No user found by username: {}", input);
        }

        Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(input.toLowerCase());
        if (userByEmail.isPresent()) {
            log.info("Found user by email: {}", userByEmail.get().getEmail());
        } else {
            log.info("No user found by email: {}", input.toLowerCase());
        }

        User user = userByUsername.orElse(userByEmail.orElse(null));
        if (user == null) {
            log.warn("Login failed: User not found for input '{}'", input);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for user '{}'", user.getUsername());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }
        log.info("Login successful for user '{}'", user.getUsername());
        return generateJwtToken(user);
    }

    // Gửi email quên mật khẩu
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        String token = UUID.randomUUID().toString();
        resetTokenStore.put(token, user.getUsername() != null ? user.getUsername() : user.getEmail());
        // Gửi email (giả lập)
        String resetUrl = resetPasswordBaseUrl + "?token=" + token;
        // Nếu có mailSender thì gửi thật, ở đây log ra
        log.info("Reset password link for {}: {}", email, resetUrl);
        //SimpleMailMessage message = new SimpleMailMessage();
        //message.setTo(email);
        //message.setSubject("Reset your password");
        //message.setText("Click the link to reset your password: " + resetUrl);
        //mailSender.send(message);
    }

    // Đặt lại mật khẩu
    public void resetPassword(String token, String newPassword) {
        String usernameOrEmail = resetTokenStore.get(token);
        if (usernameOrEmail == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }
        User user = userRepository.findByUsername(usernameOrEmail)
                .orElse(userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found")));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokenStore.remove(token);
    }

    /**
     * Get Facebook authorization URL
     * @return The authorization URL
     */
    public String getFacebookAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, "pending");
        return String.format(
            "https://www.facebook.com/v%s/dialog/oauth?client_id= %s&redirect_uri=%s&state=%s&scope=%s",
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
     * Logout current user and blacklist the JWT token
     */
    public void logout() {
        try {
            // Get the current request to extract the JWT token
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    // Blacklist the token
                    jwtTokenProvider.blacklistToken(token);
                    log.info("User logged out and token blacklisted successfully");
                } else {
                    log.warn("No valid JWT token found during logout");
                };
    }

            // Clear security context
            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            // Still clear the security context even if token blacklisting fails
            SecurityContextHolder.clearContext();
        }
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