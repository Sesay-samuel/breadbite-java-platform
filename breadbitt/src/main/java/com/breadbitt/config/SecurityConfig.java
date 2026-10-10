
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

                        // Public account and password-reset endpoints
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

                        // Public pages and static resources
                        .requestMatchers(PUBLIC_MATCHERS).permitAll()

                        // All other endpoints require login
                        .anyRequest().authenticated())

                // CSRF protection remains enabled.
                .csrf(csrf -> {
                })

                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())

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
