package com.fazbear.productos.events;

import com.fazbear.productos.repository.ProductoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalog-clp;DB_CLOSE_DELAY=-1",
    "spring.sql.init.mode=always",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "ev3.kafka.enabled=true",
    "ev3.kafka.initial-delay-ms=600000"
})
class CatalogClpMigrationTest {
    @Autowired CatalogClpMigration migration;
    @Autowired ProductoRepository products;
    @Autowired JdbcTemplate db;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean OutboxPublisher publisher;

    @Test void migratesOnlyPricesOnceAndEnqueuesNewCatalogSnapshots() {
        db.update("update productos set precio = precio / 1000, stock = mod(id, 4)");
        var before = db.queryForList("select id, nombre, stock from productos order by id");
        var tx = new TransactionTemplate(transactions);
        db.update("update productos set precio = 12.98 where nombre = 'Pizza Margherita de Freddy'");
        assertThatThrownBy(() -> tx.execute(s -> migration.migrate(products))).isInstanceOf(IllegalStateException.class);
        assertThat(db.queryForObject("select max(precio) from productos", BigDecimal.class)).isEqualByComparingTo("450");
        assertThat(db.queryForObject("select count(*) from ev3_outbox", Integer.class)).isZero();

        db.update("update productos set precio = 12.99 where nombre = 'Pizza Margherita de Freddy'");
        boolean applied = tx.execute(s -> migration.migrate(products));
        boolean repeated = tx.execute(s -> migration.migrate(products));
        assertThat(applied).isTrue();
        assertThat(repeated).isFalse();
        assertThat(db.queryForList("select id, nombre, stock from productos order by id")).isEqualTo(before);
        assertThat(db.queryForObject(
            "select precio from productos where nombre = 'Pizza Margherita de Freddy'", BigDecimal.class))
            .isEqualByComparingTo("12990");
        assertThat(db.queryForObject("select count(*) from ev3_outbox", Integer.class)).isEqualTo(23);
    }
}
