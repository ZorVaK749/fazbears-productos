package com.fazbear.productos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.resourceserver.jwt.tenant-id:ee0d2c5b-e60b-4c57-9b30-8a38a520c430}")
    private String tenantId;
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    /**
     * JwtDecoder personalizado: valida issuer, expiración Y audience
     * sin depender del formato de la propiedad audiences[0] de Spring Boot.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = jwkSetUri.isBlank() ? JwtDecoders.fromIssuerLocation(issuerUri)
            : NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Validador de audience: acepta si aud contiene el clientId
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            "aud", aud -> aud != null && aud.contains(clientId)
        );

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> tenantValidator = new JwtClaimValidator<Object>(
            "tid", tid -> tid instanceof String && tenantId.equals(tid));
        OAuth2TokenValidator<Jwt> combined  = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, tenantValidator);
        decoder.setJwtValidator(combined);

        return decoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/productos/**", "/api/productos").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/productos/*/interacciones").authenticated()
                // No administrator role is defined in the existing contract: fail closed on catalog CRUD.
                .requestMatchers(HttpMethod.POST, "/api/productos", "/api/productos/**").denyAll()
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").denyAll()
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").denyAll()
                .requestMatchers("/api/productos/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .headers(headers -> headers.frameOptions(f -> f.disable()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(
            "https://p5gsy16utd.execute-api.us-east-1.amazonaws.com",
            "http://localhost:4200"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/productos/**", cors);
        source.registerCorsConfiguration("/api/productos", cors);
        return source;
    }
}
