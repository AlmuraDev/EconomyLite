/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.config;

import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.nio.file.Path;

/**
 * A HOCON file in EconomyLite's config folder, read the way EconomyLite 2.x wrote it.
 */
public final class ConfigFile {

    private final HoconConfigurationLoader loader;
    private final Logger logger;
    private final String name;
    private CommentedConfigurationNode root;

    public ConfigFile(final Path configDir, final String name, final Logger logger) {
        this.loader = HoconConfigurationLoader.builder().path(configDir.resolve(name)).build();
        this.logger = logger;
        this.name = name;
        try {
            this.root = this.loader.load();
        } catch (final ConfigurateException e) {
            logger.error("Could not read {}, using the defaults", name, e);
            this.root = this.loader.createNode();
        }
    }

    public CommentedConfigurationNode root() {
        return this.root;
    }

    public CommentedConfigurationNode node(final Object... path) {
        return this.root.node(path);
    }

    /** Sets the value if the file has none, and the comment if the value has none. */
    public void setDefault(final String comment, final Object value, final Object... path) {
        final CommentedConfigurationNode node = this.root.node(path);
        try {
            if (node.virtual()) {
                node.set(value);
            }
        } catch (final ConfigurateException e) {
            this.logger.error("Could not set the default of {} in {}", String.join(".", toStrings(path)), this.name, e);
        }
        if (comment != null && node.comment() == null) {
            node.comment(comment);
        }
    }

    public void set(final Object value, final Object... path) {
        try {
            this.root.node(path).set(value);
        } catch (final ConfigurateException e) {
            this.logger.error("Could not set {} in {}", String.join(".", toStrings(path)), this.name, e);
        }
    }

    /** Removes a top level key. */
    public void remove(final String key) {
        this.root.removeChild(key);
    }

    public void save() {
        try {
            this.loader.save(this.root);
        } catch (final ConfigurateException e) {
            this.logger.error("Could not save {}", this.name, e);
        }
    }

    private static String[] toStrings(final Object[] path) {
        final String[] strings = new String[path.length];
        for (int i = 0; i < path.length; i++) {
            strings[i] = String.valueOf(path[i]);
        }
        return strings;
    }
}
