/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/**
 * EconomyLite's messages: {@code messages.conf} in the config folder, filled from the defaults in the jar.
 *
 * <p>As in EconomyLite 2.x, the keys in the file use dashes ({@code command-balance}) where the code uses dots
 * ({@code command.balance}), and messages use {@code &} colour codes and {@code {name}} placeholders.</p>
 */
public final class Messages {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigFile file;

    public Messages(final Path configDir, final Logger logger) {
        this.file = new ConfigFile(configDir, "messages.conf", logger);
        final Properties defaults = new Properties();
        try (InputStream in = Messages.class.getResourceAsStream("/messages.properties")) {
            if (in != null) {
                defaults.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (final IOException e) {
            logger.error("Could not read the default messages", e);
        }
        defaults.stringPropertyNames().forEach(key -> this.file.setDefault(null, defaults.getProperty(key), fileKey(key)));
        this.file.save();
    }

    private static String fileKey(final String key) {
        return key.replace('.', '-');
    }

    /**
     * Gets a message with its placeholders filled.
     *
     * @param key The message key, such as {@code command.balance}
     * @param replacements Placeholder names and their values, in pairs
     */
    public Component get(final String key, final String... replacements) {
        String message = this.file.node(fileKey(key)).getString(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return SERIALIZER.deserialize(message);
    }
}
