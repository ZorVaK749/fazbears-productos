package com.fazbear.productos.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazbear.productos.model.Producto;
import com.fazbear.productos.model.PedidoStockEvent;
import com.fazbear.productos.repository.ProductoRepository;
import com.fazbear.productos.service.ProductoService;
import com.fazbear.productos.service.StockService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"ev3.kafka.enabled=true", "spring.sql.init.mode=never",
    "spring.datasource.url=jdbc:h2:mem:product-events;DB_CLOSE_DELAY=-1",
    "spring.rabbitmq.host=localhost", "spring.rabbitmq.username=guest", "spring.rabbitmq.password=guest",
    "spring.rabbitmq.virtual-host=/", "spring.rabbitmq.listener.simple.auto-startup=false",
    "logging.level.org.springframework.security=WARN", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class ProductEventsIntegrationTest {
    static final String TENANT = "ee0d2c5b-e60b-4c57-9b30-8a38a520c430";
    static final String OWNER = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    @Autowired MockMvc mvc;
    @Autowired ProductoRepository productos;
    @Autowired ProductoService service;
    @Autowired StockService stock;
    @Autowired OutboxRepository outbox;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean OutboxPublisher publisher;
    ObjectMapper mapper = new ObjectMapper();
    Producto producto;
    @BeforeEach void setup() {
        outbox.deleteAll(); productos.deleteAll();
        producto = productos.saveAndFlush(new Producto("Actual catalog", null, new BigDecimal("17.25"), "PIZZA", null));
    }
    @Test void authenticatedViewAndSelectEmitOnlyValidatedOwner() throws Exception {
        for (String type : List.of("VIEW", "SELECT")) {
            mvc.perform(post("/api/productos/" + producto.getId() + "/interacciones")
                .with(jwt().jwt(j -> j.claim("tid", TENANT).claim("oid", OWNER).claim("scp", "access_as_user")))
                .contentType("application/json").content("{\"tipo\":\"" + type + "\",\"ownerId\":\"spoof\"}"))
                .andExpect(status().isAccepted());
        }
        var rows = jdbc.queryForList("select payload, event_key from ev3_outbox order by id");
        assertEquals(2, rows.size());
        var event = mapper.readTree((String) rows.get(0).get("payload"));
        assertEquals(9, event.size());
        UUID.fromString(event.get("eventId").asText());
        assertEquals("producto.interaccion.v1", event.get("eventType").asText());
        assertEquals("ms-productos", event.get("producer").asText());
        assertEquals(OWNER, event.at("/data/ownerId").asText());
        assertEquals(TENANT + ":" + OWNER, rows.get(0).get("event_key"));
        assertEquals("VIEW", event.at("/data/tipo").asText());
    }
    @Test void readDoesNotImplyViewAndCrudFailsClosed() throws Exception {
        mvc.perform(get("/api/productos/" + producto.getId()).with(jwt())).andExpect(status().isOk())
            .andExpect(jsonPath("$.precio").value(17.25)).andExpect(jsonPath("$.stock").value(20));
        mvc.perform(post("/api/productos").with(jwt()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/productos/" + producto.getId()).with(jwt()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/productos/" + producto.getId()).with(jwt())).andExpect(status().isForbidden());
        assertEquals(0, outbox.count());
    }
    @Test void invalidIdentityAndMissingProductDoNotEmit() throws Exception {
        String path = "/api/productos/" + producto.getId() + "/interacciones";
        mvc.perform(post(path).contentType("application/json").content("{\"tipo\":\"VIEW\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(jwt()).contentType("application/json").content("{\"tipo\":\"VIEW\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/productos/999999/interacciones")
            .with(jwt().jwt(j -> j.claim("tid", TENANT).claim("oid", OWNER).claim("scp", "access_as_user")))
            .contentType("application/json").content("{\"tipo\":\"SELECT\"}"))
            .andExpect(status().isNotFound());
        assertEquals(0, outbox.count());
    }
    @Test void stockRabbitFlowPublishesActualSnapshotAtomicallyAndDeduplicates() throws Exception {
        var event = new PedidoStockEvent(UUID.randomUUID().toString(), 42L, 2,
            List.of(new PedidoStockEvent.ItemStock(producto.getId(), 3)));
        assertTrue(stock.procesar(event)); assertFalse(stock.procesar(event));
        assertEquals(1, outbox.count());
        var payload = mapper.readTree(jdbc.queryForObject("select payload from ev3_outbox", String.class));
        assertEquals("producto.actualizado.v1", payload.get("eventType").asText());
        assertEquals(17, payload.at("/data/stock").asInt());
        assertEquals(new BigDecimal("17.25"), payload.at("/data/precio").decimalValue());
        assertTrue(payload.at("/data/activo").asBoolean());
    }
    @Test void catalogAndOutboxRollbackTogether() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.save(new Producto("Rollback", null, BigDecimal.ONE, "PIZZA", null));
            assertEquals(1, outbox.count()); status.setRollbackOnly();
        });
        assertEquals(0, outbox.count()); assertEquals(1, productos.count());
    }
}
