package com.fazbear.productos.config;

import com.fazbear.productos.model.PedidoStockEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {
    @Test
    void declaresDurableStockQueueAndDeadLetterBinding() {
        var declarations = new RabbitMQConfig().stockTopology();
        var queues = declarations.getDeclarablesByType(Queue.class);
        assertEquals(2, queues.size());
        Queue main = queues.stream().filter(q -> q.getName().equals(RabbitMQConfig.QUEUE)).findFirst().orElseThrow();
        assertTrue(main.isDurable());
        assertEquals("pedidos.dlx", main.getArguments().get("x-dead-letter-exchange"));
        var bindings = declarations.getDeclarablesByType(Binding.class);
        assertTrue(bindings.stream().anyMatch(b -> b.getDestination().equals(RabbitMQConfig.QUEUE)
            && b.getExchange().equals("pedidos.exchange") && b.getRoutingKey().equals("pedido.creado")));
        assertTrue(bindings.stream().anyMatch(b -> b.getDestination().equals(RabbitMQConfig.QUEUE + ".dlq")
            && b.getExchange().equals("pedidos.dlx") && b.getRoutingKey().equals(RabbitMQConfig.QUEUE)));
    }

    @Test
    void convertsPublisherV2JsonToLocalStockRecord() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setHeader("__TypeId__", "com.fazbear.orden.model.PedidoEvent");
        properties.setInferredArgumentType(PedidoStockEvent.class);
        String json = """
            {"eventoId":"9abe44c5-206a-4231-9ba0-14cd3101f42f","pedidoId":1,"schemaVersion":2,
             "emailUsuario":"user@example.com","total":12.99,"items":[{"productoId":3,"cantidad":2}]}
            """;
        PedidoStockEvent event = assertInstanceOf(PedidoStockEvent.class,
            new RabbitMQConfig().messageConverter().fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), properties)));
        assertEquals(2, event.schemaVersion());
        assertEquals(3L, event.items().get(0).productoId());
        assertEquals(2, event.items().get(0).cantidad());
    }
}
