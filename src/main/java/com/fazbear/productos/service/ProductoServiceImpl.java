package com.fazbear.productos.service;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta del servicio de Productos.
 * Delega las operaciones de persistencia al repositorio JPA.
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
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
    public Producto save(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public void deleteById(Long id) {
        productoRepository.deleteById(id);
    }
}
