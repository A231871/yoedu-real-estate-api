package com.yoedu.yoedurealestateapi.config;

import com.yoedu.yoedurealestateapi.security.AppJwtProperties;
import com.yoedu.yoedurealestateapi.security.JwtAccessDeniedHandler;
import com.yoedu.yoedurealestateapi.security.JwtAuthenticationEntryPoint;
import com.yoedu.yoedurealestateapi.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(AppJwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtAuthenticationEntryPoint authenticationEntryPoint,
        JwtAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                    .requestMatchers(HttpMethod.GET, "/auth/verify").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.GET, "/reviews").permitAll()
                    .requestMatchers(HttpMethod.GET, "/listing/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/property-type").permitAll()
                    .requestMatchers(HttpMethod.GET, "/location/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/amenity").permitAll()
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/ws/**" // WebSocket SockJS handshake — JWT auth via STOMP interceptor
                    ).permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
            // TODO: Development CORS, please change during production
            List.of(
                "http://localhost:5173", // Frontend origin
                "https://localhost:8080" // Backend origin
            )
        );

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
            List.of("Authorization", "Content-Type", "Accept")
        );
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
