package com.fazbear.productos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.rabbitmq.host=localhost", "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest", "spring.rabbitmq.virtual-host=/",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "logging.level.org.springframework.security=WARN"
})
class ProductosApplicationTests {

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
