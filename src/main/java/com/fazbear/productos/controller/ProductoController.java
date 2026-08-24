package com.fazbear.productos.controller;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el catálogo de productos de Freddy Fazbear's Pizza.
 * Expone endpoints bajo /api/productos
 */
@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * GET /api/productos
     * Lista todos los productos del menú.
     */
    @GetMapping
    public ResponseEntity<List<Producto>> getAll() {
        return ResponseEntity.ok(productoService.findAll());
    }

    /**
     * GET /api/productos/{id}
     * Retorna un producto específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Producto> getById(@PathVariable Long id) {
        return productoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/productos/categoria/{categoria}
     * Filtra por categoría: PIZZA, BEBIDA, SOUVENIR, ANIMATRONICO
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Producto>> getByCategoria(@PathVariable String categoria) {
        List<Producto> productos = productoService.findByCategoria(categoria);
        return ResponseEntity.ok(productos);
    }

    /**
     * POST /api/productos
     * Crea un nuevo producto en el catálogo.
     */
    @PostMapping
    public ResponseEntity<Producto> create(@RequestBody Producto producto) {
        Producto saved = productoService.save(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/productos/{id}
     * Actualiza un producto existente por ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Producto> update(@PathVariable Long id,
                                           @RequestBody Producto producto) {
        return productoService.findById(id)
                .map(existing -> {
                    producto.setId(id);
                    return ResponseEntity.ok(productoService.save(producto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/productos/{id}
     * Elimina un producto del catálogo.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (productoService.findById(id).isEmpty()) {
            return ResponseEntity.<Void>notFound().build();
        }
        productoService.deleteById(id);
        return ResponseEntity.<Void>noContent().build();
    }
}
