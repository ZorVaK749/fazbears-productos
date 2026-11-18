package com.fazbear.productos;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de arranque real utilizando el perfil efectivo del contenedor Docker (ev3-aws)
 * con almacenamiento H2 persistente en archivo y semilla idempotente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("ev3-aws")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:file:./target/test-data-effective/productosdb;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.sql.init.mode=always",
    "ev3.kafka.enabled=true",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.main.allow-bean-definition-overriding=true"
})
@Transactional
class ProductoEffectiveProfileStartupTest {

    @TestConfiguration
    static class StubJwtDecoderConfig {
        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return token -> null;
        }
    }

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    @DisplayName("El perfil efectivo ev3-aws debe inicializar exactamente los 23 productos con 20 unidades cada uno")
    void testCatalogInitializationUnderEffectiveProfile() {
        List<Producto> productos = productoRepository.findAll();
        assertThat(productos).hasSize(23);

        // Verificar que todas las unidades iniciales son 20
        assertThat(productos).allMatch(p -> p.getStock() == 20);

        // Simular descuento de stock
        Producto p1 = productos.get(0);
        int rowsUpdated = productoRepository.descontarStock(p1.getId(), 5);
        assertThat(rowsUpdated).isEqualTo(1);

        Producto reloaded = productoRepository.findById(p1.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(15);
    }
}
