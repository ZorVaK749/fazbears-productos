package com.fazbear.productos.events;

import com.fazbear.productos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;

/** Explicit local-only opt-ins. Never replaces a nonempty catalog. */
@Component
@Profile("ev3-local")
public class LocalCatalogBootstrap implements CommandLineRunner {
    private final ProductoRepository productos;
    private final CatalogEvents events;
    private final DataSource dataSource;
    @Value("${ev3.local.seed:false}") private boolean seed;
    @Value("${ev3.local.catalog-snapshot:false}") private boolean snapshot;
    public LocalCatalogBootstrap(ProductoRepository productos, CatalogEvents events, DataSource dataSource) {
        this.productos = productos; this.events = events; this.dataSource = dataSource;
    }
    @Override
    @Transactional
    public void run(String... args) {
        if (seed && productos.count() == 0) {
            new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        }
        if (snapshot) {
            productos.findAll().stream().map(p -> p.getId()).sorted().forEach(id ->
                productos.findForUpdate(id).ifPresent(p -> events.snapshot(p, true)));
        }
    }
}
