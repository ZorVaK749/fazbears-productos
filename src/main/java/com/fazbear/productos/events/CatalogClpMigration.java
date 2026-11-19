package com.fazbear.productos.events;

import com.fazbear.productos.repository.ProductoRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** One-time repricing of the EV3 menu. Called only during the approved AWS cutover. */
@Component
class CatalogClpMigration {
    private static final String VERSION = "clp-v1";
    private final JdbcTemplate db;
    private final CatalogEvents events;

    CatalogClpMigration(JdbcTemplate db, CatalogEvents events) {
        this.db = db;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    boolean migrate(ProductoRepository products) {
        // H2 auto-commits DDL. Create the empty marker table before locking or changing any rows.
        db.execute("create table if not exists catalog_price_migration (version varchar(32) primary key)");
        if (applied()) return false;

        Map<String, Price> prices = prices();
        var rows = products.catalogIds().stream()
            .map(id -> products.findForUpdate(id).orElseThrow()).toList();
        if (applied()) return false;
        // Validate all names and source prices before any change. Keep the live IDs and quantities.
        var remaining = new HashMap<>(prices);
        for (var row : rows) {
            var price = remaining.remove(row.getNombre());
            if (price == null || row.getStock() < 0 || row.getPrecio() == null
                    || (row.getPrecio().compareTo(price.previous()) != 0
                        && row.getPrecio().compareTo(price.clp()) != 0)) {
                throw new IllegalStateException("Unexpected product or catalog price. Review the backup before repricing.");
            }
        }
        if (!remaining.isEmpty()) throw new IllegalStateException("Expected the complete 23-product EV3 menu");

        for (var row : rows) {
            row.setPrecio(prices.get(row.getNombre()).clp());
            events.snapshot(row, true);
        }
        db.update("insert into catalog_price_migration(version) values (?)", VERSION);
        return true;
    }

    private boolean applied() {
        return db.queryForObject("select count(*) from catalog_price_migration where version = ?",
            Integer.class, VERSION) == 1;
    }

    private static Map<String, Price> prices() {
        var resource = new ClassPathResource("precios-clp-v1.tsv");
        String priceList;
        try {
            priceList = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("CLP price list unavailable", e);
        }
        var prices = new HashMap<String, Price>();
        for (String line : priceList.lines().toList()) {
            String[] columns = line.split("\t");
            prices.put(columns[0], new Price(new BigDecimal(columns[1]), new BigDecimal(columns[2])));
        }
        if (prices.size() != 23) throw new IllegalStateException("Expected 23 entries in the CLP price list");
        return prices;
    }

    private record Price(BigDecimal previous, BigDecimal clp) {}
}
