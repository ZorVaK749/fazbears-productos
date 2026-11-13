package com.fazbear.productos.events;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.service.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "EV3_DOCKER_TEST", matches = "true")
@ActiveProfiles("ev3-local")
@SpringBootTest(properties = {"ev3.kafka.enabled=true", "ev3.kafka.initial-delay-ms=3600000",
    "spring.rabbitmq.listener.simple.auto-startup=false"})
class PostgresKafkaIntegrationTest {
    @Autowired ProductoService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxPublisher publisher;
    @MockitoBean JwtDecoder decoder;
    @Test void actualPostgresCatalogSnapshotReachesRealKafka() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertTrue(connection.getMetaData().getURL().contains("/ev3_productos_test"), "Disposable test DB required");
        }
        Producto producto = service.save(new Producto("Explicit synthetic integration test", null,
            new BigDecimal("9.25"), "PIZZA", null));
        String key = producto.getId().toString();
        String payload = jdbc.queryForObject("select payload from ev3_outbox where event_key = ?", String.class, key);
        publisher.publish();
        assertTrue(jdbc.queryForObject("select delivered from ev3_outbox where event_key = ?", Boolean.class, key));
        var config = new HashMap<String, Object>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "ev3-test-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var consumer = new KafkaConsumer<String, String>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of("producto.actualizado.v1"));
            long deadline = System.nanoTime() + Duration.ofSeconds(25).toNanos();
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().equals(payload)) {
                        assertEquals(key, record.key());
                        var event = new ObjectMapper().readTree(record.value());
                        assertEquals(20, event.at("/data/stock").asInt());
                        assertEquals(new BigDecimal("9.25"), event.at("/data/precio").decimalValue());
                        return;
                    }
                }
            }
            fail("Committed catalog snapshot not observed in real Kafka");
        }
    }
}
