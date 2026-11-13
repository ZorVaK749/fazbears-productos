package com.fazbear.productos.events;

import com.fazbear.productos.model.Producto;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CatalogEvents {
    private final EventOutbox outbox;
    public CatalogEvents(EventOutbox outbox) { this.outbox = outbox; }
    public void snapshot(Producto producto, boolean activo) {
        outbox.append("producto.actualizado.v1", producto.getId().toString(), Map.of(
            "productoId", producto.getId(), "nombre", producto.getNombre(),
            "categoria", producto.getCategoria(), "precio", producto.getPrecio(),
            "stock", producto.getStock(), "activo", activo));
    }
}
