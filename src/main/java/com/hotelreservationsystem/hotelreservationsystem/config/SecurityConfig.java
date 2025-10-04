package com.hotelreservationsystem.hotelreservationsystem.config;

import com.hotelreservationsystem.hotelreservationsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication) {

                String role = authentication.getAuthorities().iterator().next().getAuthority();

                if ("ROLE_ADMIN".equals(role)) {
                    return "/dashboard";
                } else if ("ROLE_STAFF".equals(role)) {
                    return "/receptionist/dashboard";
                } else {
                    return "/dashboard";
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Static resources - completely ignore from security
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()
                        
                        // Public pages
                        .requestMatchers("/", "/index", "/home").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/rooms").permitAll()
                        .requestMatchers("/promotions", "/promotions/**").permitAll()
                        
                        .requestMatchers("/error").permitAll()

                        // Custom payment endpoints - require authentication
                        .requestMatchers("/api/custom-payment/**").authenticated()

                        // Protected pages - require authentication
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/booking/**").authenticated()
                        .requestMatchers("/payment/**").authenticated()
                        .requestMatchers("/reviews/**").authenticated()
                        .requestMatchers("/my-bookings").authenticated()
                        .requestMatchers("/profile").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/receptionist/**").hasRole("STAFF")

                        // API endpoints
                        .requestMatchers("/api/bookings/**").authenticated()
                        .requestMatchers("/api/payment/alternative").authenticated()
                        .requestMatchers("/api/rooms/available").permitAll()
                        

                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .csrf(csrf -> {})
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                                        "https://www.google-analytics.com https://www.googletagmanager.com " +
                                        "https://maxcdn.bootstrapcdn.com https://cdnjs.cloudflare.com " +
                                        "https://unpkg.com https://cdn.jsdelivr.net; " +
                                        "style-src 'self' 'unsafe-inline' " +
                                        "https://maxcdn.bootstrapcdn.com https://cdnjs.cloudflare.com " +
                                        "https://fonts.googleapis.com https://cdn.jsdelivr.net; " +
                                        "font-src 'self' https://fonts.gstatic.com " +
                                        "https://maxcdn.bootstrapcdn.com https://cdnjs.cloudflare.com; " +
                                        "img-src 'self' data: " +
                                        "https://www.google-analytics.com; " +
                                        "connect-src 'self' " +
                                        "https://www.google-analytics.com https://analytics.google.com " +
                                        "https://cdn.jsdelivr.net https://unpkg.com; " +
                                        "frame-src 'self'; " +
                                        "form-action 'self';")
                        )
                );

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}