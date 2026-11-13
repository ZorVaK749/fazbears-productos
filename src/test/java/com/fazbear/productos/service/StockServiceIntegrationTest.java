package com.fazbear.productos.service;

import com.fazbear.productos.model.PedidoStockEvent;
import com.fazbear.productos.model.PedidoStockEvent.ItemStock;
import com.fazbear.productos.model.Producto;
import com.fazbear.productos.repository.ProductoRepository;
import com.fazbear.productos.repository.StockEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.sql.init.mode=never", "spring.jpa.show-sql=false"}, showSql = false)
@Import({StockService.class, com.fazbear.productos.events.CatalogEvents.class,
    com.fazbear.productos.events.EventOutbox.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockServiceIntegrationTest {
    @Autowired StockService service;
    @Autowired ProductoRepository productos;
    @Autowired StockEventRepository procesados;

    @BeforeEach
    void clean() {
        procesados.deleteAll();
        productos.deleteAll();
    }

    private Producto producto(int stock) {
        Producto producto = new Producto("Pizza", "Test", new BigDecimal("12.99"), "PIZZA", null);
        producto.setStock(stock);
        return productos.saveAndFlush(producto);
    }

    private PedidoStockEvent event(ItemStock... items) {
        return new PedidoStockEvent(UUID.randomUUID().toString(), 1L, 2, Arrays.asList(items));
    }

    private int stock(Long id) {
        return productos.findById(id).orElseThrow().getStock();
    }

    @Test
    void discountsEveryProductInOneEvent() {
        Producto a = producto(20);
        Producto b = producto(20);
        assertTrue(service.procesar(event(new ItemStock(a.getId(), 3), new ItemStock(b.getId(), 2))));
        assertEquals(17, stock(a.getId()));
        assertEquals(18, stock(b.getId()));
        assertEquals(1, procesados.count());
    }

    @Test
    void aggregatesRepeatedProductLines() {
        Producto producto = producto(20);
        service.procesar(event(new ItemStock(producto.getId(), 2), new ItemStock(producto.getId(), 3)));
        assertEquals(15, stock(producto.getId()));
    }

    @Test
    void insufficientStockRollsBackAllProductsAndTheIdempotencyRecord() {
        Producto a = producto(20);
        Producto b = producto(0);
        PedidoStockEvent event = event(new ItemStock(a.getId(), 1), new ItemStock(b.getId(), 1));
        assertThrows(StockEventException.class, () -> service.procesar(event));
        assertEquals(20, stock(a.getId()));
        assertEquals(0, stock(b.getId()));
        assertEquals(0, procesados.count());
        b.setStock(20);
        productos.saveAndFlush(b);
        assertTrue(service.procesar(event));
        assertEquals(19, stock(a.getId()));
        assertEquals(19, stock(b.getId()));
    }

    @Test
    void missingProductDoesNotLeavePartialDiscounts() {
        Producto a = producto(20);
        assertThrows(StockEventException.class, () -> service.procesar(
            event(new ItemStock(a.getId(), 1), new ItemStock(Long.MAX_VALUE, 1))));
        assertEquals(20, stock(a.getId()));
        assertEquals(0, procesados.count());
    }

    @Test
    void redeliveryDoesNotDiscountAgain() {
        Producto producto = producto(20);
        PedidoStockEvent event = event(new ItemStock(producto.getId(), 2));
        assertTrue(service.procesar(event));
        assertFalse(service.procesar(event));
        assertEquals(18, stock(producto.getId()));
        assertEquals(1, procesados.count());
    }

    @Test
    void sameEventIdWithDifferentContentsIsRejected() {
        Producto producto = producto(20);
        PedidoStockEvent event = event(new ItemStock(producto.getId(), 2));
        service.procesar(event);
        PedidoStockEvent altered = new PedidoStockEvent(event.eventoId(), 1L, 2,
            List.of(new ItemStock(producto.getId(), 3)));
        assertThrows(StockEventException.class, () -> service.procesar(altered));
        assertEquals(18, stock(producto.getId()));
    }

    static Stream<List<ItemStock>> invalidItems() {
        return Stream.of(null, List.of(), Arrays.asList((ItemStock) null),
            List.of(new ItemStock(null, 1)), List.of(new ItemStock(1L, 0)),
            List.of(new ItemStock(1L, -1)), List.of(new ItemStock(1L, null)),
            List.of(new ItemStock(0L, 1)),
            List.of(new ItemStock(1L, Integer.MAX_VALUE), new ItemStock(1L, 1)));
    }

    @ParameterizedTest
    @MethodSource("invalidItems")
    void malformedQuantitiesNeverModifyStock(List<ItemStock> items) {
        PedidoStockEvent event = new PedidoStockEvent(UUID.randomUUID().toString(), 1L, 2, items);
        assertThrows(StockEventException.class, () -> service.procesar(event));
        assertEquals(0, procesados.count());
    }

    @Test
    void legacyEventsWithoutTheStockContractAreRejected() {
        assertThrows(StockEventException.class, () -> service.procesar(
            new PedidoStockEvent(null, 1L, null, null)));
        assertThrows(StockEventException.class, () -> service.procesar(
            new PedidoStockEvent("not-a-uuid", 1L, 2, List.of(new ItemStock(1L, 1)))));
        assertEquals(0, procesados.count());
    }

    @Test
    void concurrentRedeliveriesOnlyDiscountOnce() throws Exception {
        Producto producto = producto(20);
        PedidoStockEvent event = event(new ItemStock(producto.getId(), 2));
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Future<Boolean> a = executor.submit(() -> { start.await(); return service.procesar(event); });
            Future<Boolean> b = executor.submit(() -> { start.await(); return service.procesar(event); });
            start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(18, stock(producto.getId()));
            assertEquals(1, procesados.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void competingOrdersCannotMakeStockNegative() throws Exception {
        Producto producto = producto(1);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Future<Boolean> a = executor.submit(() -> {
                start.await(); return service.procesar(event(new ItemStock(producto.getId(), 1)));
            });
            Future<Boolean> b = executor.submit(() -> {
                start.await(); return service.procesar(event(new ItemStock(producto.getId(), 1)));
            });
            start.countDown();
            int successes = 0;
            int rejected = 0;
            for (Future<Boolean> result : List.of(a, b)) {
                try {
                    if (result.get(15, TimeUnit.SECONDS)) successes++;
                } catch (ExecutionException error) {
                    assertInstanceOf(StockEventException.class, error.getCause());
                    rejected++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, rejected);
            assertEquals(0, stock(producto.getId()));
            assertEquals(1, procesados.count());
        } finally {
            executor.shutdownNow();
        }
    }
}
