package com.fazbear.productos.events;

import com.fazbear.productos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;

/** Enqueues a snapshot from the existing catalog. Seeding is confined to the lab profile. */
@Component
@Profile({"ev3-local", "ev3-aws"})
public class CatalogBootstrap implements CommandLineRunner {
    private final ProductoRepository productos;
    private final CatalogEvents events;
    private final DataSource dataSource;
    private final Environment environment;
    @Value("${ev3.local.seed:false}") private boolean seed;
    @Value("${ev3.local.catalog-snapshot:false}") private boolean labSnapshot;
    @Value("${ev3.aws.catalog-snapshot:false}") private boolean awsSnapshot;
    @Value("${ev3.kafka.enabled:false}") private boolean kafkaEnabled;

    public CatalogBootstrap(ProductoRepository productos, CatalogEvents events, DataSource dataSource,
                            Environment environment) {
        this.productos = productos; this.events = events; this.dataSource = dataSource;
        this.environment = environment;
    }
    @Override
    @Transactional
    public void run(String... args) {
        boolean aws = environment.matchesProfiles("ev3-aws");
        if (!aws && seed && productos.count() == 0) {
            new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        }
        if (aws ? awsSnapshot : labSnapshot) {
            var ids = productos.findAll().stream().map(p -> p.getId()).sorted().toList();
            if (aws && (!kafkaEnabled || ids.size() != 23)) {
                throw new IllegalStateException("AWS catalog snapshot requires Kafka and exactly 23 restored products");
            }
            for (long id : ids) {
                events.snapshot(productos.findForUpdate(id).orElseThrow(), true);
            }
        }
    }
}
