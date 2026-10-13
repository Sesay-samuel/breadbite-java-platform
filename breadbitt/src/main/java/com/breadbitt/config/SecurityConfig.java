
package com.breadbitt.config;

import com.breadbitt.service.impl.UserSecurityService;
import com.breadbitt.utility.SecurityUtility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserSecurityService userSecurityService;

    // Validate Keycloak identity mapping after OIDC authentication.
    @Autowired
    private KeycloakLoginSuccessHandler keycloakLoginSuccessHandler;

    private BCryptPasswordEncoder passwordEncoder() {
        return SecurityUtility.passwordEncoder();
    }

    private static final String[] PUBLIC_MATCHERS = {
            "/",
            "/login",
            "/css/**",
            "/js/**",
            "/image/**",
            "/images/**",
            "/fonts/**",
            "/webjars/**",
            "/breadShelf",
            "/breadDetail",
            "/breadDetail/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Public registration and password-reset endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/newUser",
                                "/resetPassword")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/newUser",
                                "/forgetPassword",
                                "/resetPassword")
                        .permitAll()

                        // Spring Security OAuth2 authorization endpoints
                        .requestMatchers(
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**")
                        .permitAll()

                        // Public pages and static resources
                        .requestMatchers(PUBLIC_MATCHERS)
                        .permitAll()

                        // All other endpoints require authentication
                        .anyRequest()
                        .authenticated())

                // Keep CSRF protection enabled
                .csrf(csrf -> {
                })

                // Existing Breadbite username/password login
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())

                // Keycloak OpenID Connect login with identity mapping check
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(keycloakLoginSuccessHandler))

                // Existing logout behavior
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())

                // Existing remember-me functionality
                .rememberMe(rememberMe -> rememberMe
                        .userDetailsService(userSecurityService)
                        .tokenValiditySeconds(86400));

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth)
            throws Exception {

        auth.userDetailsService(userSecurityService)
                .passwordEncoder(passwordEncoder());
    }
}
