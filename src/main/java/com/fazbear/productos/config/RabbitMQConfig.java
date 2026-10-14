package com.fazbear.productos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "pedidos.exchange";
    public static final String QUEUE = "pedido.creado.productos.queue";
    public static final String ROUTING_KEY = "pedido.creado";

    @Bean
    public Declarables stockTopology() {
        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange("pedidos.dlx", true, false);
        Queue queue = QueueBuilder.durable(QUEUE)
            .deadLetterExchange("pedidos.dlx").deadLetterRoutingKey(QUEUE).build();
        Queue dlq = QueueBuilder.durable(QUEUE + ".dlq").build();
        return new Declarables(exchange, dlx, queue, dlq,
            BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY),
            BindingBuilder.bind(dlq).to(dlx).with(QUEUE));
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }
}
