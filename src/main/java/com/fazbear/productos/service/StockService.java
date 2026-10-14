package com.fazbear.productos.service;

import com.fazbear.productos.model.PedidoStockEvent;
import com.fazbear.productos.model.StockEventProcesado;
import com.fazbear.productos.repository.ProductoRepository;
import com.fazbear.productos.repository.StockEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class StockService {
    private final ProductoRepository productos;
    private final StockEventRepository procesados;
    private final TransactionTemplate transaction;

    public StockService(ProductoRepository productos, StockEventRepository procesados,
                        PlatformTransactionManager transactionManager) {
        this.productos = productos;
        this.procesados = procesados;
        this.transaction = new TransactionTemplate(transactionManager);
        // El catch de duplicados se ejecuta después del rollback, no en una transacción invalidada.
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** @return true si se descontó; false si esta misma entrega ya estaba confirmada. */
    public boolean procesar(PedidoStockEvent event) {
        Map<Long, Integer> cantidades = validar(event);
        String huella = huella(event.pedidoId(), cantidades);
        try {
            return Boolean.TRUE.equals(transaction.execute(status -> {
                var existente = procesados.findById(event.eventoId());
                if (existente.isPresent()) {
                    verificarHuella(existente.get(), huella);
                    return false;
                }
                // PK único: dos consumidores del mismo evento no pueden confirmarlo dos veces.
                procesados.saveAndFlush(new StockEventProcesado(event.eventoId(), event.pedidoId(), huella));
                // Orden estable de locks y suma de líneas repetidas del mismo producto.
                for (var item : cantidades.entrySet()) {
                    if (productos.descontarStock(item.getKey(), item.getValue()) != 1) {
                        throw new StockEventException("Producto inexistente o stock insuficiente: " + item.getKey());
                    }
                }
                return true;
            }));
        } catch (DataIntegrityViolationException conflict) {
            // Una entrega concurrente pudo confirmar mientras esta esperaba el mismo PK.
            var confirmado = procesados.findById(event.eventoId());
            if (confirmado.isPresent()) {
                verificarHuella(confirmado.get(), huella);
                return false;
            }
            throw conflict;
        }
    }

    private Map<Long, Integer> validar(PedidoStockEvent event) {
        if (event == null || event.eventoId() == null || event.eventoId().length() != 36) {
            throw new StockEventException("Falta eventoId UUID de pedido.creado v2");
        }
        try {
            if (!UUID.fromString(event.eventoId()).toString().equals(event.eventoId())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException invalid) {
            throw new StockEventException("eventoId no es un UUID válido");
        }
        if (!Integer.valueOf(2).equals(event.schemaVersion()) || event.pedidoId() == null
                || event.pedidoId() <= 0 || event.items() == null || event.items().isEmpty()) {
            throw new StockEventException("Evento v2 sin pedido o items válidos");
        }
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (var item : event.items()) {
            if (item == null || item.productoId() == null || item.productoId() <= 0
                    || item.cantidad() == null || item.cantidad() <= 0) {
                throw new StockEventException("Cada item requiere productoId y cantidad positivos");
            }
            try {
                cantidades.merge(item.productoId(), item.cantidad(), Math::addExact);
            } catch (ArithmeticException overflow) {
                throw new StockEventException("Cantidad acumulada fuera de rango");
            }
        }
        return cantidades;
    }

    private void verificarHuella(StockEventProcesado existente, String huella) {
        if (!existente.getHuella().equals(huella)) {
            throw new StockEventException("eventoId repetido con un contenido diferente");
        }
    }

    private String huella(Long pedidoId, Map<Long, Integer> cantidades) {
        StringBuilder canonical = new StringBuilder().append(pedidoId).append('|');
        cantidades.forEach((id, cantidad) -> canonical.append(id).append(':').append(cantidad).append(';'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }
}
