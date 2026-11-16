package com.fazbear.productos.events;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.repository.ProductoRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Nimbus signature validation against ephemeral loopback JWKS; never Azure. */
@SpringBootTest(properties = {"spring.sql.init.mode=never", "spring.jpa.show-sql=false",
    "spring.datasource.url=jdbc:h2:mem:products-jwt;DB_CLOSE_DELAY=-1",
    "spring.rabbitmq.host=localhost", "spring.rabbitmq.username=guest", "spring.rabbitmq.password=guest",
    "spring.rabbitmq.virtual-host=/", "spring.rabbitmq.listener.simple.auto-startup=false",
    "logging.level.org.springframework.security=WARN"})
@AutoConfigureMockMvc
class ProductoJwtIntegrationTest {
    static final String TENANT = "ee0d2c5b-e60b-4c57-9b30-8a38a520c430";
    static final String ISSUER = "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    static final String AUDIENCE = "304fa289-2b72-447b-9c48-f00998f7c22f";
    static final RSAKey KEY;
    static final HttpServer JWKS;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("local-only").generate();
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            JWKS.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var response = exchange.getResponseBody()) { response.write(body); }
                exchange.close();
            });
            JWKS.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> JWKS.stop(0)));
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks");
    }
    @Autowired MockMvc mvc;
    @Autowired ProductoRepository productos;
    Long id;
    @BeforeEach void seedExplicitTestProduct() {
        id = productos.saveAndFlush(new Producto("JWT test only", null, BigDecimal.ONE, "PIZZA", null)).getId();
    }
    String token(Map<String, Object> overrides, RSAKey key) throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER); claims.put("aud", AUDIENCE); claims.put("tid", TENANT);
        claims.put("oid", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"); claims.put("scp", "access_as_user");
        claims.put("exp", Date.from(Instant.now().plusSeconds(300))); claims.putAll(overrides);
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder(); claims.forEach(builder::claim);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("local-only").build(), builder.build());
        jwt.sign(new RSASSASigner(key)); return jwt.serialize();
    }
    @Test void validSignedDelegatedJwtCanInteractAndReadCatalog() throws Exception {
        String bearer = token(Map.of(), KEY);
        mvc.perform(get("/api/productos/" + id).header("Authorization", "Bearer " + bearer))
            .andExpect(status().isOk());
        mvc.perform(post("/api/productos/" + id + "/interacciones").header("Authorization", "Bearer " + bearer)
            .contentType("application/json").content("{\"tipo\":\"VIEW\"}"))
            .andExpect(status().isAccepted());
    }
    @Test void rejectsInvalidIssuerAudienceTenantExpiryAndSignature() throws Exception {
        for (var overrides : List.<Map<String, Object>>of(Map.of("iss", "https://invalid.example"),
                Map.of("aud", "wrong-audience"), Map.of("tid", UUID.randomUUID().toString()),
                Map.of("exp", Date.from(Instant.now().minusSeconds(3600))))) {
            mvc.perform(get("/api/productos/" + id).header("Authorization", "Bearer " + token(overrides, KEY)))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/productos/" + id).header("Authorization", "Bearer " +
            token(Map.of(), new RSAKeyGenerator(2048).generate()))).andExpect(status().isUnauthorized());
    }
    @Test void signedButInvalidOidOrScopeCannotInteract() throws Exception {
        for (var overrides : List.<Map<String, Object>>of(Map.of("oid", "email@example.test"),
                Map.of("oid", ""), Map.of("scp", "other_access_as_user"), Map.of("scp", ""))) {
            mvc.perform(post("/api/productos/" + id + "/interacciones")
                .header("Authorization", "Bearer " + token(overrides, KEY))
                .contentType("application/json").content("{\"tipo\":\"VIEW\"}"))
                .andExpect(status().isForbidden());
        }
    }

    @Test void corsPreflightAndActualRequestTests() throws Exception {
        String frontend = "https://p5gsy16utd.execute-api.us-east-1.amazonaws.com";
        // 1. OPTIONS preflight with allowed origin
        mvc.perform(options("/api/productos/" + id + "/interacciones")
                .header("Origin", frontend)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type,accept,origin,x-requested-with"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", frontend));

        // 2. Unauthorized origin rejected
        mvc.perform(options("/api/productos")
                .header("Origin", "https://unauthorized.evil.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        // 3. GET protected without token returns 401
        mvc.perform(get("/api/productos/" + id))
            .andExpect(status().isUnauthorized());

        // 4. GET protected without token with allowed origin retains CORS header
        mvc.perform(get("/api/productos/" + id).header("Origin", frontend))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("Access-Control-Allow-Origin", frontend));

        // 5. Valid JWT with allowed origin succeeds
        String bearer = token(Map.of(), KEY);
        mvc.perform(get("/api/productos/" + id).header("Origin", frontend).header("Authorization", "Bearer " + bearer))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", frontend));
    }
}
