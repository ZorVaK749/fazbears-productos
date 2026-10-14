package com.fazbear.productos.listener;

import com.fazbear.productos.config.RabbitMQConfig;
import com.fazbear.productos.model.PedidoStockEvent;
import com.fazbear.productos.service.StockEventException;
import com.fazbear.productos.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoListener {
    private static final Logger log = LoggerFactory.getLogger(PedidoListener.class);
    private final StockService stockService;

    public PedidoListener(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onPedidoCreado(PedidoStockEvent event) {
        try {
            boolean changed = stockService.procesar(event);
            log.info("Stock {}: eventoId={}, pedidoId={}", changed ? "descontado" : "ya procesado",
                event.eventoId(), event.pedidoId());
        } catch (StockEventException invalid) {
            log.error("Evento de stock rechazado: {}", invalid.getMessage());
            throw new AmqpRejectAndDontRequeueException("Evento no aplicable al stock", invalid);
        }
    }
}
