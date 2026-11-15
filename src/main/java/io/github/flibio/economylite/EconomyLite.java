/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite;

import com.google.inject.Inject;
import io.github.flibio.economylite.api.CurrencyEconService;
import io.github.flibio.economylite.api.PlayerEconService;
import io.github.flibio.economylite.api.VirtualEconService;
import io.github.flibio.economylite.commands.Commands;
import io.github.flibio.economylite.config.ConfigFile;
import io.github.flibio.economylite.config.Messages;
import io.github.flibio.economylite.impl.CurrencyService;
import io.github.flibio.economylite.impl.PlayerDataService;
import io.github.flibio.economylite.impl.VirtualDataService;
import io.github.flibio.economylite.impl.economy.LiteEconomyService;
import io.github.flibio.economylite.storage.Database;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.math.BigDecimal;
import java.nio.file.Path;

@Plugin(EconomyLite.ID)
public final class EconomyLite {

    public static final String ID = "economylite";

    private static EconomyLite instance;

    private static ConfigFile config;
    private static Messages messages;
    private static Database database;
    private static CurrencyService currencyService;
    private static PlayerEconService playerService;
    private static VirtualEconService virtualService;
    private static LiteEconomyService economyService;

    private final PluginContainer container;
    private final Logger logger;
    private final Path configDir;

    @Inject
    public EconomyLite(final PluginContainer container, final Logger logger, @ConfigDir(sharedRoot = false) final Path configDir) {
        this.container = container;
        this.logger = logger;
        this.configDir = configDir;
    }

    @Listener
    public void onConstruct(final ConstructPluginEvent event) {
        instance = this;
        this.logger.info("EconomyLite {} is initializing!", this.container.metadata().version());

        config = new ConfigFile(this.configDir, "config.conf", this.logger);
        config.setDefault("Default balance of player accounts.", 0.0, "default-balance", "player");
        config.setDefault("Default balance of virtual accounts.", 0.0, "default-balance", "virtual");
        config.setDefault("Enables or disables debug logging.", true, "debug-logging");
        config.setDefault("If enabled players will be notified when an admin changes their balance", false, "notify-on-admin-commands");
        config.setDefault("If enabled players will be asked to confirm payments to an offline player.", true, "confirm-offline-payments");
        config.save();

        currencyService = new CurrencyService(new ConfigFile(this.configDir, "currencies.conf", this.logger), this.logger);
        messages = new Messages(this.configDir, this.logger);

        try {
            database = Database.open(this.configDir, this.logger);
        } catch (final Exception e) {
            this.logger.error("EconomyLite could not open its database, so it will not provide the economy", e);
            return;
        }
        final boolean debug = config.node("debug-logging").getBoolean(false);
        playerService = new PlayerDataService(database, this.logger, debug);
        virtualService = new VirtualDataService(database, this.logger, debug);
        economyService = new LiteEconomyService();
        this.logger.info("Using currency: {}", PlainTextComponentSerializer.plainText().serialize(currencyService.getCurrentCurrency().displayName()));
    }

    @Listener
    public void onRegisterCurrencies(final RegisterRegistryValueEvent.GameScoped event) {
        event.registry(RegistryTypes.CURRENCY, step -> currencyService.registerAll(step::register));
    }

    @Listener
    public void onProvideEconomy(final ProvideServiceEvent.EngineScoped<EconomyService, Server> event) {
        if (economyService != null) {
            event.suggest(() -> economyService);
        }
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        if (economyService != null) {
            Commands.register(event, this.container);
        }
    }

    @Listener
    public void onServerStopping(final StoppingEngineEvent<Server> event) {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Path getConfigDir() {
        return this.configDir;
    }

    public PluginContainer getPluginContainer() {
        return this.container;
    }

    public static EconomyLite getInstance() {
        return instance;
    }

    public static ConfigFile getConfig() {
        return config;
    }

    public static Messages getMessages() {
        return messages;
    }

    public static CurrencyEconService getCurrencyService() {
        return currencyService;
    }

    public static PlayerEconService getPlayerService() {
        return playerService;
    }

    public static VirtualEconService getVirtualService() {
        return virtualService;
    }

    public static EconomyService getEconomyService() {
        return economyService;
    }

    public static BigDecimal getDefaultPlayerBalance() {
        return BigDecimal.valueOf(config.node("default-balance", "player").getDouble(0));
    }

    public static BigDecimal getDefaultVirtualBalance() {
        return BigDecimal.valueOf(config.node("default-balance", "virtual").getDouble(0));
    }
}
