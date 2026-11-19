package com.fazbear.productos.repository;

import com.fazbear.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Producto.
 * Hereda operaciones CRUD completas de JpaRepository.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Query("select p.id from Producto p order by p.id")
    List<Long> catalogIds();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    java.util.Optional<Producto> findForUpdate(@Param("id") Long id);

    /**
     * Busca productos por categoría (case-insensitive).
     * Ejemplo: findByCategoria("PIZZA")
     */
    List<Producto> findByCategoriaIgnoreCase(String categoria);

    /** Una única actualización condicional evita stock negativo incluso con concurrencia. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.id = :id AND p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
