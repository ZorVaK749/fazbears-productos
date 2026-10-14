package com.fazbear.productos.service;

/** Evento inválido o inventario insuficiente: conservar en DLQ, nunca descontar parcialmente. */
public class StockEventException extends RuntimeException {
    public StockEventException(String message) {
        super(message);
    }
}
