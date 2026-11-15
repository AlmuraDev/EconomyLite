/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.storage;

import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * EconomyLite's H2 database, {@code data.mv.db} in the plugin's config folder, as EconomyLite 2.x kept it.
 *
 * <p>The tables keep the layout of EconomyLite 2.x:
 * {@code economyliteplayers(uuid VARCHAR(36), balance DECIMAL(11,2), currency VARCHAR(1024))} and
 * {@code economylitevirts(id VARCHAR(1024), balance DECIMAL(11,2), currency VARCHAR(1024))}.
 * A database written by EconomyLite 2.x (H2 1.4) is moved to H2 2 the first time it is opened.</p>
 *
 * <p>One connection stays open for the life of the server; every call on it is synchronized.</p>
 */
public final class Database implements AutoCloseable {

    /** H2's error code for a database file written by another major version. */
    private static final int UNSUPPORTED_FILE_VERSION = 90048;

    private static final String NAME = "data";

    private final Logger logger;
    private final Connection connection;

    private Database(final Logger logger, final Connection connection) {
        this.logger = logger;
        this.connection = connection;
    }

    public static Database open(final Path configDir, final Logger logger) throws Exception {
        final H2Drivers drivers = new H2Drivers(configDir.resolve("lib"));
        final Driver driver = drivers.current();
        final Path base = configDir.toAbsolutePath().resolve(NAME);
        Connection connection;
        try {
            connection = connect(driver, base);
        } catch (final SQLException e) {
            if (e.getErrorCode() != UNSUPPORTED_FILE_VERSION) {
                throw e;
            }
            migrateFromH2v1(drivers.legacy(), driver, base, logger);
            connection = connect(driver, base);
        }
        final Database database = new Database(logger, connection);
        database.createTables();
        return database;
    }

    private static Connection connect(final Driver driver, final Path base) throws SQLException {
        // EconomyLite 2.x's database has the one user "" (admin) with no password
        final Properties info = new Properties();
        info.setProperty("user", "");
        info.setProperty("password", "");
        final Connection connection = driver.connect("jdbc:h2:" + base.toString().replace('\\', '/'), info);
        if (connection == null) {
            throw new SQLException("The H2 driver refused the database URL");
        }
        return connection;
    }

    /**
     * Moves EconomyLite 2.x's H2 1.4 database to H2 2: H2 1.4 writes it out as SQL, H2 2 reads that into a new file.
     * The old file stays next to it as {@code data.mv.db.h2-1.4.bak}.
     */
    private static void migrateFromH2v1(final Driver legacy, final Driver current, final Path base, final Logger logger) throws Exception {
        final Path dir = base.getParent();
        final Path file = dir.resolve(NAME + ".mv.db");
        final Path backup = dir.resolve(NAME + ".mv.db.h2-1.4.bak");
        final Path script = dir.resolve(NAME + "-h2-1.4-export.sql");
        logger.info("Moving the EconomyLite database from H2 1.4 to H2 2, the old file is kept as {}", backup.getFileName());
        if (Files.exists(backup)) {
            throw new IllegalStateException(backup + " already exists, so an earlier move to H2 2 did not finish."
                    + " Check which file holds the balances before starting again.");
        }

        long players;
        long virtuals;
        BigDecimal total;
        try (Connection old = connect(legacy, base); Statement statement = old.createStatement()) {
            players = count(statement, "SELECT COUNT(*) FROM economyliteplayers");
            virtuals = count(statement, "SELECT COUNT(*) FROM economylitevirts");
            total = sum(statement);
            statement.execute("SCRIPT TO '" + sqlPath(script) + "'");
        }

        Files.move(file, backup, StandardCopyOption.ATOMIC_MOVE);
        try (Connection moved = connect(current, base); Statement statement = moved.createStatement()) {
            statement.execute("RUNSCRIPT FROM '" + sqlPath(script) + "' FROM_1X");
            final long newPlayers = count(statement, "SELECT COUNT(*) FROM economyliteplayers");
            final long newVirtuals = count(statement, "SELECT COUNT(*) FROM economylitevirts");
            final BigDecimal newTotal = sum(statement);
            if (newPlayers != players || newVirtuals != virtuals || newTotal.compareTo(total) != 0) {
                throw new IllegalStateException("The moved EconomyLite database does not match the old one (players " + players + " -> "
                        + newPlayers + ", virtual accounts " + virtuals + " -> " + newVirtuals + ", player total " + total + " -> " + newTotal
                        + "). The old file is " + backup);
            }
            logger.info("Moved {} player accounts and {} virtual accounts to H2 2", newPlayers, newVirtuals);
        } catch (final Exception e) {
            // Put the old file back, so the next start tries again from the untouched data
            Files.deleteIfExists(file);
            Files.move(backup, file, StandardCopyOption.ATOMIC_MOVE);
            throw e;
        }
        Files.deleteIfExists(script);
    }

    private static String sqlPath(final Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/').replace("'", "''");
    }

    private static long count(final Statement statement, final String sql) throws SQLException {
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static BigDecimal sum(final Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("SELECT SUM(balance) FROM economyliteplayers")) {
            final BigDecimal sum = rs.next() ? rs.getBigDecimal(1) : null;
            return sum == null ? BigDecimal.ZERO : sum;
        }
    }

    private void createTables() throws SQLException {
        try (Statement statement = this.connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS economyliteplayers(uuid VARCHAR(36), balance DECIMAL(11,2), currency VARCHAR(1024))");
            statement.execute("CREATE TABLE IF NOT EXISTS economylitevirts(id VARCHAR(1024), balance DECIMAL(11,2), currency VARCHAR(1024))");
            // EconomyLite 2.x first made the id 36 long and widened it later
            statement.execute("ALTER TABLE economylitevirts ALTER COLUMN id SET DATA TYPE VARCHAR(1024)");
        }
    }

    public synchronized boolean update(final String sql, final Object... args) {
        try (PreparedStatement ps = this.prepare(sql, args)) {
            ps.executeUpdate();
            return true;
        } catch (final SQLException e) {
            this.logger.error("EconomyLite database update failed: {}", sql, e);
            return false;
        }
    }

    public synchronized Optional<BigDecimal> queryDecimal(final String sql, final Object... args) {
        try (PreparedStatement ps = this.prepare(sql, args); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.ofNullable(rs.getBigDecimal(1)) : Optional.empty();
        } catch (final SQLException e) {
            this.logger.error("EconomyLite database query failed: {}", sql, e);
            return Optional.empty();
        }
    }

    /** Empty when the query failed, as opposed to an answer of no rows. */
    public synchronized Optional<Boolean> queryExists(final String sql, final Object... args) {
        try (PreparedStatement ps = this.prepare(sql, args); ResultSet rs = ps.executeQuery()) {
            return Optional.of(rs.next());
        } catch (final SQLException e) {
            this.logger.error("EconomyLite database query failed: {}", sql, e);
            return Optional.empty();
        }
    }

    public synchronized List<String> queryStrings(final String sql, final Object... args) {
        final List<String> values = new ArrayList<>();
        try (PreparedStatement ps = this.prepare(sql, args); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        } catch (final SQLException e) {
            this.logger.error("EconomyLite database query failed: {}", sql, e);
        }
        return values;
    }

    private PreparedStatement prepare(final String sql, final Object... args) throws SQLException {
        final PreparedStatement ps = this.connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }

    @Override
    public synchronized void close() {
        try {
            this.connection.close();
        } catch (final SQLException e) {
            this.logger.error("Failed to close the EconomyLite database", e);
        }
    }
}
