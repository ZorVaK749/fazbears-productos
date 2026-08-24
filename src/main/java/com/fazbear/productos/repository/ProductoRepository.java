package com.fazbear.productos.repository;

import com.fazbear.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Producto.
 * Hereda operaciones CRUD completas de JpaRepository.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Busca productos por categoría (case-insensitive).
     * Ejemplo: findByCategoria("PIZZA")
     */
    List<Producto> findByCategoriaIgnoreCase(String categoria);
}
