package com.example.Graduation_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/", "/login/**", "/oauth2/**", "/main/**", "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            String redirectUrl = getRedirectBaseUrl() + "?login=success";
                            response.sendRedirect(redirectUrl);
                        })
                        .failureHandler((request, response, exception) -> {
                            String redirectUrl = getRedirectBaseUrl() + "?login=failure";
                            response.sendRedirect(redirectUrl);
                        })
                );

        return http.build();
    }

    private String getRedirectBaseUrl() {
        return System.getenv().getOrDefault("REDIRECT_BASE_URL", "http://localhost:3000");
    }
}
