package com.fbadsautomation.config;

import com.fbadsautomation.security.JwtAuthenticationFilter;
import com.fbadsautomation.service.AuthService;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    private final AuthService authService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(AuthService authService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authService = authService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers(
		"/api/health",
                "api/health",
                "/api/auth/register",
                "/api/auth/login-app",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/api/auth/oauth2/**",
                "/api/public/**",
                "/api/ai-providers/**",
                "/api/images/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()
            .antMatchers("/api/auth/user").authenticated()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .authorizationEndpoint()
            .baseUri("/api/auth/oauth2/authorize")
            .and()
            .redirectionEndpoint()
            .baseUri("/api/auth/oauth2/callback/*")
            .and()
            .userInfoEndpoint()
            .and()
            .successHandler(oauth2AuthenticationSuccessHandler())
            .failureHandler(oauth2AuthenticationFailureHandler());
            
        // Add JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                    Authentication authentication) throws IOException, ServletException {
                log.info("OAuth2 authentication successful");
                
                try {
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal(); // Process the OAuth2 user and create/update our user
                    String token = authService.processOAuth2User(oauth2User);
                    // Redirect to frontend with token
                    String redirectUrl = "https://linhnh.site/auth-success#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
                    response.sendRedirect(redirectUrl);
                    
                } catch (Exception e) {
                    log.error("Error processing OAuth2 authentication: {}", e.getMessage(), e);
                    response.sendRedirect("https://linhnh.site/login?error=auth_process_failed");
                }
            };
    };
    }

    @Bean
    public AuthenticationFailureHandler oauth2AuthenticationFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                    org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
                log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);
                response.sendRedirect("https://linhnh.site/login?error=auth_failed");
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "https://linhnh.site",
            "https://api.linhnh.site"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
