package com.fazbear.productos.controller;

import com.fazbear.productos.events.EventOutbox;
import com.fazbear.productos.repository.ProductoRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/productos")
public class ProductoInteraccionController {
    public enum Tipo { VIEW, SELECT }
    public record Interaccion(Tipo tipo) { }
    private final ProductoRepository productos;
    private final EventOutbox outbox;
    private final String tenant;
    public ProductoInteraccionController(ProductoRepository productos, EventOutbox outbox,
            @Value("${spring.security.oauth2.resourceserver.jwt.tenant-id:ee0d2c5b-e60b-4c57-9b30-8a38a520c430}") String tenant) {
        this.productos = productos;
        this.outbox = outbox;
        this.tenant = tenant;
    }
    @PostMapping("/{id}/interacciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public void interact(@PathVariable Long id, @RequestBody Interaccion request,
                         @AuthenticationPrincipal Jwt jwt) {
        String owner = requireOwner(jwt);
        if (request.tipo() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (!productos.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        outbox.append("producto.interaccion.v1", tenant + ":" + owner,
            Map.of("tenantId", tenant, "ownerId", owner, "productoId", id, "tipo", request.tipo().name()));
    }
    private String requireOwner(Jwt jwt) {
        if (jwt == null || !tenant.equals(jwt.getClaims().get("tid"))
                || !(jwt.getClaims().get("oid") instanceof String oid)
                || !(jwt.getClaims().get("scp") instanceof String scopes)
                || Arrays.stream(scopes.split("\\s+")).noneMatch("access_as_user"::equals)) {
            throw new AccessDeniedException("Identidad delegada requerida");
        }
        try {
            if (!UUID.fromString(oid).toString().equalsIgnoreCase(oid)) throw new IllegalArgumentException();
            return oid;
        } catch (IllegalArgumentException invalid) {
            throw new AccessDeniedException("Identidad delegada requerida");
        }
    }
}
