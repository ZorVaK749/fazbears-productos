package com.fazbear.productos.model;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductoStockTest {
    @Test
    void defaultStockIsTwentyAndNegativeStockIsInvalid() {
        Producto producto = new Producto();
        assertEquals(20, producto.getStock());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            producto.setStock(-1);
            assertFalse(factory.getValidator().validate(producto).isEmpty());
            producto.setStock(0);
            assertTrue(factory.getValidator().validate(producto).isEmpty());
        }
    }
}
