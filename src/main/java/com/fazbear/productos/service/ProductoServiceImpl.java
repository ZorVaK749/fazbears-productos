package com.fazbear.productos.service;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fazbear.productos.events.CatalogEvents;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta del servicio de Productos.
 * Delega las operaciones de persistencia al repositorio JPA.
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CatalogEvents events;

    public ProductoServiceImpl(ProductoRepository productoRepository, CatalogEvents events) {
        this.productoRepository = productoRepository;
        this.events = events;
    }

    @Override
    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return productoRepository.findById(id);
    }

    @Override
    public List<Producto> findByCategoria(String categoria) {
        return productoRepository.findByCategoriaIgnoreCase(categoria);
    }

    @Override
    @Transactional
    public Producto save(Producto producto) {
        if (producto.getId() != null) productoRepository.findForUpdate(producto.getId());
        Producto saved = productoRepository.saveAndFlush(producto);
        events.snapshot(saved, true);
        return saved;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        productoRepository.findForUpdate(id).ifPresent(p -> events.snapshot(p, false));
        productoRepository.deleteById(id);
    }
}
