/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl;

import io.github.flibio.economylite.CauseFactory;
import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.api.CurrencyEconService;
import io.github.flibio.economylite.config.ConfigFile;
import io.github.flibio.economylite.impl.economy.LiteCurrency;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The currencies of {@code currencies.conf}: {@code coin} (the default one, always there), any created with
 * {@code /currency create}, and {@code current}, the key of the one in use.
 */
public final class CurrencyService implements CurrencyEconService {

    private static final String DEFAULT_KEY = "coin";

    private final ConfigFile file;
    private final Logger logger;
    private final Set<Currency> currencies = Collections.synchronizedSet(new LinkedHashSet<>());
    private final LiteCurrency defaultCurrency;
    private volatile Currency currentCurrency;
    private volatile boolean registryOpen;

    public CurrencyService(final ConfigFile file, final Logger logger) {
        this.file = file;
        this.logger = logger;
        file.setDefault(null, DEFAULT_KEY, "current");
        file.setDefault(null, "Coin", DEFAULT_KEY, "singular");
        file.setDefault(null, "Coins", DEFAULT_KEY, "plural");
        file.setDefault(null, "C", DEFAULT_KEY, "symbol");
        this.defaultCurrency = this.read(DEFAULT_KEY, true).orElseGet(() -> new LiteCurrency("Coin", "Coins", "C", true, 2));
        this.currencies.add(this.defaultCurrency);
        this.currentCurrency = this.defaultCurrency;

        for (final Map.Entry<Object, CommentedConfigurationNode> entry : file.root().childrenMap().entrySet()) {
            final String key = String.valueOf(entry.getKey());
            if (!key.equals(DEFAULT_KEY) && entry.getValue().isMap()) {
                this.read(key, false).ifPresent(this.currencies::add);
            }
        }

        final String current = file.node("current").getString("");
        final Optional<Currency> found = this.byConfigKey(current);
        if (found.isPresent()) {
            this.currentCurrency = found.get();
        } else {
            // The current currency could not be found, fall back to the default one
            file.set(DEFAULT_KEY, "current");
        }
        file.save();
    }

    private Optional<LiteCurrency> read(final String key, final boolean isDefault) {
        final String singular = this.file.node(key, "singular").getString("");
        final String plural = this.file.node(key, "plural").getString("");
        final String symbol = this.file.node(key, "symbol").getString("");
        if (singular.isEmpty() || plural.isEmpty() || symbol.isEmpty()) {
            if (!isDefault) {
                this.logger.warn("The currency {} in currencies.conf needs a singular, plural and symbol, skipping it", key);
            }
            return Optional.empty();
        }
        return Optional.of(new LiteCurrency(singular, plural, symbol, isDefault, 2));
    }

    private Optional<Currency> byConfigKey(final String key) {
        synchronized (this.currencies) {
            return this.currencies.stream().filter(c -> ((LiteCurrency) c).configKey().equalsIgnoreCase(key)).findFirst();
        }
    }

    /** Puts every currency in Sponge's currency registry; called while Sponge registers game registry values. */
    public void registerAll(final java.util.function.BiConsumer<ResourceKey, Currency> registrar) {
        synchronized (this.currencies) {
            this.currencies.forEach(c -> this.register(c, registrar));
        }
        this.registryOpen = true;
    }

    private void register(final Currency currency, final java.util.function.BiConsumer<ResourceKey, Currency> registrar) {
        final LiteCurrency lite = (LiteCurrency) currency;
        final Optional<ResourceKey> key = lite.registryKey();
        if (key.isEmpty()) {
            this.logger.warn("The currency {} has a name Sponge's currency registry can not hold, other plugins will not find it there",
                    lite.singular());
            return;
        }
        try {
            registrar.accept(key.get(), currency);
        } catch (final RuntimeException e) {
            this.logger.warn("Could not add the currency {} to Sponge's currency registry", lite.singular(), e);
        }
    }

    @Override
    public void addCurrency(final Currency currency) {
        this.currencies.add(currency);
        if (currency instanceof LiteCurrency lite) {
            final String key = lite.configKey();
            this.file.set(lite.singular(), key, "singular");
            this.file.set(lite.plural(), key, "plural");
            this.file.set(lite.symbolText(), key, "symbol");
            this.file.save();
            if (this.registryOpen) {
                this.register(currency, (k, c) -> RegistryTypes.CURRENCY.get().register(k, c));
            }
        }
    }

    @Override
    public Set<Currency> getCurrencies() {
        synchronized (this.currencies) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(this.currencies));
        }
    }

    @Override
    public Currency getDefaultCurrency() {
        return this.defaultCurrency;
    }

    @Override
    public void setCurrentCurrency(final Currency currency) {
        this.currentCurrency = currency;
        this.file.set(((LiteCurrency) currency).configKey(), "current");
        this.file.save();
    }

    @Override
    public Currency getCurrentCurrency() {
        return this.currentCurrency;
    }

    @Override
    public void deleteCurrency(final Currency currency) {
        if (currency.equals(this.defaultCurrency)) {
            return;
        }
        EconomyLite.getPlayerService().clearCurrency(currency, CauseFactory.create("Currency deletion"));
        EconomyLite.getVirtualService().clearCurrency(currency, CauseFactory.create("Currency deletion"));
        this.currencies.remove(currency);
        // Sponge's registry can not drop a value; the currency stays there until the server restarts
        this.file.remove(((LiteCurrency) currency).configKey());
        this.file.save();
    }

    @Override
    public String getId(final Currency currency) {
        if (currency instanceof LiteCurrency lite) {
            return lite.id();
        }
        return currency.findKey(RegistryTypes.CURRENCY).map(ResourceKey::asString).orElseGet(() -> currency.displayName().toString());
    }
}
