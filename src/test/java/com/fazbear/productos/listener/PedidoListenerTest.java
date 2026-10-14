package com.fazbear.productos.listener;

import com.fazbear.productos.model.PedidoStockEvent;
import com.fazbear.productos.service.StockEventException;
import com.fazbear.productos.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoListenerTest {
    @Test
    void businessFailureIsRejectedWithoutRequeue() {
        StockService service = mock(StockService.class);
        PedidoStockEvent event = new PedidoStockEvent(UUID.randomUUID().toString(), 1L, 2, List.of());
        when(service.procesar(event)).thenThrow(new StockEventException("Stock insuficiente"));
        assertThrows(AmqpRejectAndDontRequeueException.class,
            () -> new PedidoListener(service).onPedidoCreado(event));
    }

    @Test
    void duplicateIsAcknowledgedNormally() {
        StockService service = mock(StockService.class);
        PedidoStockEvent event = new PedidoStockEvent(UUID.randomUUID().toString(), 1L, 2, List.of());
        when(service.procesar(event)).thenReturn(false);
        assertDoesNotThrow(() -> new PedidoListener(service).onPedidoCreado(event));
        verify(service).procesar(event);
    }
}
