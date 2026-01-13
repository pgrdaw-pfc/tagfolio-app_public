package com.pgrdaw.tagfolio.config;

import com.pgrdaw.tagfolio.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    /**
     * Constructs a new SecurityConfig.
     *
     * @param userDetailsService The custom user details service.
     */
    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Defines the array of public paths that do not require authentication.
     *
     * @return A String array of public URL patterns.
     */
    private String[] publicRequestMatchers() {
        return new String[]{
                "/",
                "/login",
                "/register",
                "/seeding",
                "/license",
                "/help/**",
                "/shared/filter/**",
                "/shared/report/**",
                "/public/**",
                "/css/**",
                "/js/**",
                "/images/original/**",
                "/images/thumbnail/**",
                "/favicon.ico",
                "/favicon.svg",
                "/apple-touch-icon.png",
                "/images/tags",
                "/api/filters",
                "/images/filter",
                "/images/paginated",
                "/images/by-ids",
                "/api/reports/types",
                "/images/export",
                "/images/{id}"
        };
    }

    /**
     * Configures the security filter chain.
     *
     * @param http The {@link HttpSecurity} to configure.
     * @return The configured {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                            .requestMatchers(publicRequestMatchers()).permitAll()
                            .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .userDetailsService(userDetailsService)
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Provides the authentication manager bean.
     *
     * @param config The authentication configuration.
     * @return The {@link AuthenticationManager}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
