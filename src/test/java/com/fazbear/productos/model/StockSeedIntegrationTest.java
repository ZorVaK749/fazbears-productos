package com.fazbear.productos.model;

import com.fazbear.productos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = "spring.jpa.show-sql=false")
class StockSeedIntegrationTest {
    @Autowired ProductoRepository productos;

    @Test
    void allDemoProductsStartWithTwentyUnits() {
        var seeds = productos.findAll();
        assertEquals(23, seeds.size());
        assertTrue(seeds.stream().allMatch(producto -> producto.getStock() == 20));
    }
}
