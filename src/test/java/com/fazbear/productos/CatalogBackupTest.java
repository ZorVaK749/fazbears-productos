package com.fazbear.productos;

import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogBackupTest {
    @TempDir
    Path temp;

    @Test
    void restoresEveryProductWithItsOriginalStockIncludingZero() throws Exception {
        String url = "jdbc:h2:file:" + temp.resolve("source");
        try (var db = DriverManager.getConnection(url, "sa", "test-catalog");
             var sql = db.createStatement()) {
            sql.execute("""
                create table productos (id bigint primary key, nombre varchar not null,
                    descripcion varchar, precio decimal(10,2), categoria varchar, imagen_url varchar, stock int)
                """);
            sql.execute("""
                insert into productos select X, 'Pizza ' || X, 'Descripción; ' || X,
                    1000.35 + X, 'PIZZA', null, X - 1 from system_range(1, 23)
                """);
        }
        var backup = temp.resolve("backup");
        Path h2Jar = Path.of(Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        var run = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", h2Jar.toString(), "deploy/CatalogBackup.java", "backup", backup.toString())
                .redirectErrorStream(true).redirectOutput(temp.resolve("backup.log").toFile());
        run.environment().put("CATALOG_H2_URL", url);
        run.environment().put("CATALOG_H2_USER", "sa");
        run.environment().put("CATALOG_H2_PASSWORD", "test-catalog");
        var process = run.start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Catalog backup timed out");
        }
        assertThat(process.exitValue()).withFailMessage(Files.readString(temp.resolve("backup.log"))).isZero();
        assertThat(Files.readString(backup.resolve("catalogo.sha256")).strip()).matches("[0-9a-f]{64}");
        try (var db = DriverManager.getConnection("jdbc:h2:file:" + backup.resolve("productosdb") + ";IFEXISTS=TRUE",
                     "sa", "test-catalog");
             var sql = db.createStatement();
             var rows = sql.executeQuery("select sum(stock), min(stock), count(*) from productos")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(253);
            assertThat(rows.getInt(2)).isZero();
            assertThat(rows.getInt(3)).isEqualTo(23);
        }
    }
}
