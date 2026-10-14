package com.fazbear.productos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Vista de inventario del evento pedido.creado v2, independiente del paquete publicador. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoStockEvent(String eventoId, Long pedidoId, Integer schemaVersion,
                               List<ItemStock> items) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemStock(Long productoId, Integer cantidad) {}
}
