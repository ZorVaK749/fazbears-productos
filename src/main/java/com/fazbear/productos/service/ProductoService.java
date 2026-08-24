package com.fazbear.productos.service;

import com.fazbear.productos.model.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz de servicio para la lógica de negocio de Productos.
 */
public interface ProductoService {

    /** Retorna todos los productos del menú */
    List<Producto> findAll();

    /** Busca un producto por su ID */
    Optional<Producto> findById(Long id);

    /** Filtra productos por categoría (PIZZA, BEBIDA, SOUVENIR, ANIMATRONICO) */
    List<Producto> findByCategoria(String categoria);

    /** Crea o actualiza un producto */
    Producto save(Producto producto);

    /** Elimina un producto por ID */
    void deleteById(Long id);
}
