package com.fazbear.productos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para MS Productos.
 *
 * MODO DESARROLLO LOCAL:
 *   - Todos los endpoints /api/productos/** son públicos (sin JWT).
 *   - Permite pruebas directas en Postman sin token de Azure.
 *
 * MODO PRODUCCIÓN (con Azure Entra ID):
 *   - Descomentar la sección oauth2ResourceServer y commentar la línea permitAll.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ── DESARROLLO LOCAL: todo público ──────────────────────────
                .requestMatchers("/api/productos/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()

                // ── PRODUCCIÓN: descomentar esto y comentar las líneas de arriba ──
                // .requestMatchers("/api/productos/**").authenticated()
                // .anyRequest().denyAll()
            )
            // ── PRODUCCIÓN: descomentar para activar validación JWT de Azure ──
            // .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .headers(headers -> headers.frameOptions(f -> f.disable())); // Para H2 console

        return http.build();
    }
}
