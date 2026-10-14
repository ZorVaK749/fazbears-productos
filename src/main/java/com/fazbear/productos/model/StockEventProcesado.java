package com.fazbear.productos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/** Se confirma en la misma transacción que el descuento: redelivery no descuenta otra vez. */
@Entity
@Table(name = "stock_eventos_procesados")
public class StockEventProcesado {
    @Id
    @Column(length = 36)
    private String eventoId;

    // Con versión null, Spring Data usa persist (no merge) para el UUID asignado.
    @Version
    private Long version;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false, length = 64)
    private String huella;

    @Column(nullable = false)
    private LocalDateTime procesadoEn;

    protected StockEventProcesado() {}

    public StockEventProcesado(String eventoId, Long pedidoId, String huella) {
        this.eventoId = eventoId;
        this.pedidoId = pedidoId;
        this.huella = huella;
        this.procesadoEn = LocalDateTime.now();
    }

    public String getEventoId() { return eventoId; }
    public String getHuella() { return huella; }
}
