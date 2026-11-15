/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.util.Properties;

/**
 * Loads the H2 drivers bundled in the plugin jar, each in a class loader of its own.
 *
 * <p>SpongeAPI 21 has no SQL service, so EconomyLite brings its own H2. Keeping it out of the
 * plugin's class path means it can never clash with an H2 another mod brings.</p>
 */
final class H2Drivers {

    private static final String LIBS = "/META-INF/economylite/libs/";

    private final Path libDir;
    private final Properties versions = new Properties();

    H2Drivers(final Path libDir) {
        this.libDir = libDir;
        try (InputStream in = H2Drivers.class.getResourceAsStream("/economylite-build.properties")) {
            if (in == null) {
                throw new IllegalStateException("economylite-build.properties is missing from the EconomyLite jar");
            }
            this.versions.load(in);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** H2 2, which holds EconomyLite's data. */
    Driver current() throws Exception {
        return this.load(this.versions.getProperty("h2"));
    }

    /** H2 1.4.196, which only reads the database EconomyLite 2.x (SpongeAPI 7) left behind. */
    Driver legacy() throws Exception {
        return this.load(this.versions.getProperty("h2_legacy"));
    }

    private Driver load(final String version) throws Exception {
        final String name = "h2-" + version + ".jar";
        final Path jar = this.libDir.resolve(name);
        try (InputStream in = H2Drivers.class.getResourceAsStream(LIBS + name)) {
            if (in == null) {
                throw new IllegalStateException(name + " is missing from the EconomyLite jar");
            }
            Files.createDirectories(this.libDir);
            // Always refresh it, so an updated EconomyLite jar never runs an older H2 left on disk.
            // The file name carries the version, so a jar Windows still holds open is the same H2.
            try {
                Files.copy(in, jar, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                if (!Files.isRegularFile(jar)) {
                    throw e;
                }
            }
        }
        // The platform class loader still sees java.sql, but nothing of Minecraft, mods or plugins
        final URLClassLoader loader = new URLClassLoader("economylite-" + name, new URL[] {jar.toUri().toURL()},
                ClassLoader.getPlatformClassLoader());
        return (Driver) Class.forName("org.h2.Driver", true, loader).getDeclaredConstructor().newInstance();
    }
}
