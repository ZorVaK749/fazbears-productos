import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HexFormat;
import org.h2.tools.RunScript;

/**
 * Operator tool for a quiesced H2 source reachable through approved JDBC access.
 * The output directory must be new and inside an operator-only backup location.
 */
public class CatalogBackup {
    private static final String CATALOG = """
            SELECT id, nombre, descripcion, precio, categoria, imagen_url, stock
            FROM productos ORDER BY id
            """;

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("fingerprint")) {
            try (Connection source = connect()) {
                System.out.println(fingerprint(source));
            }
        } else if (args.length == 2 && args[0].equals("backup")) {
            backup(Path.of(args[1]).toAbsolutePath());
        } else {
            throw new IllegalArgumentException("Use backup NEW_DIRECTORY or fingerprint");
        }
    }

    private static Connection connect() throws SQLException {
        String url = System.getenv("CATALOG_H2_URL");
        String user = System.getenv("CATALOG_H2_USER");
        String password = System.getenv("CATALOG_H2_PASSWORD");
        if (url == null || !url.matches("jdbc:h2:(file:|tcp://|ssl://)[^;]+")
                || user == null || user.isBlank() || password == null) {
            throw new IllegalArgumentException("Configure CATALOG_H2_URL, CATALOG_H2_USER and CATALOG_H2_PASSWORD");
        }
        return DriverManager.getConnection(url + ";IFEXISTS=TRUE", user, password);
    }

    private static void backup(Path directory) throws Exception {
        String expected;
        Path dump = directory.resolve("productos.sql");
        try (Connection source = connect()) {
            expected = fingerprint(source);
            Files.createDirectory(directory);
            try (var statement = source.createStatement();
                 var script = statement.executeQuery("SCRIPT");
                 var writer = Files.newBufferedWriter(dump, StandardOpenOption.CREATE_NEW)) {
                while (script.next()) {
                    writer.write(script.getString(1));
                    writer.newLine();
                }
            }
            if (!expected.equals(fingerprint(source))) {
                throw new IllegalStateException("Live catalog changed during backup");
            }
        }
        String restoreUrl = "jdbc:h2:file:" + directory.resolve("productosdb");
        try (Connection restored = DriverManager.getConnection(restoreUrl,
                     System.getenv("CATALOG_H2_USER"), System.getenv("CATALOG_H2_PASSWORD"));
             var reader = Files.newBufferedReader(dump)) {
            RunScript.execute(restored, reader);
            if (!expected.equals(fingerprint(restored))) {
                throw new IllegalStateException("Restored catalog differs from the source");
            }
        }
        try (var input = new java.security.DigestInputStream(Files.newInputStream(dump), sha256())) {
            input.transferTo(OutputStream.nullOutputStream());
            String checksum = HexFormat.of().formatHex(input.getMessageDigest().digest());
            Files.writeString(directory.resolve("productos.sql.sha256"), checksum + "  productos.sql\n",
                    StandardOpenOption.CREATE_NEW);
        }
        Files.writeString(directory.resolve("catalogo.sha256"), expected + "\n", StandardOpenOption.CREATE_NEW);
        System.out.println("Verified catalog and stock for 23 products.");
    }

    private static String fingerprint(Connection connection) throws Exception {
        var digest = sha256();
        int count = 0;
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery(CATALOG);
             var output = new DataOutputStream(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
            while (rows.next()) {
                ++count;
                var price = rows.getBigDecimal("precio");
                int stock = rows.getInt("stock");
                if (rows.wasNull() || stock < 0 || price == null) {
                    throw new IllegalStateException("Incomplete or invalid catalog stock or price");
                }
                for (int column = 1; column <= 7; column++) {
                    String value = column == 4 ? price.stripTrailingZeros().toPlainString() : rows.getString(column);
                    byte[] bytes = value == null ? null : value.getBytes(StandardCharsets.UTF_8);
                    output.writeInt(bytes == null ? -1 : bytes.length);
                    if (bytes != null) {
                        output.write(bytes);
                    }
                }
            }
        }
        if (count != 23) {
            throw new IllegalStateException("Expected 23 products, found " + count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() throws java.security.NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }
}
