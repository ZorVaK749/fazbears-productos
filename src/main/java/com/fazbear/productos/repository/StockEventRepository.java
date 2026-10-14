package com.fazbear.productos.repository;

import com.fazbear.productos.model.StockEventProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEventRepository extends JpaRepository<StockEventProcesado, String> {}
