/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.service.economy.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

public final class LiteCurrency implements Currency {

    private final String singular;
    private final String plural;
    private final String symbol;
    private final boolean defaultCurrency;
    private final int digits;

    public LiteCurrency(final String singular, final String plural, final String symbol, final boolean defaultCurrency, final int digits) {
        this.singular = singular;
        this.plural = plural;
        this.symbol = symbol;
        this.defaultCurrency = defaultCurrency;
        this.digits = digits;
    }

    /**
     * The id EconomyLite stores balances under, as EconomyLite 2.x made it: {@code economylite:} and the lower case
     * singular name with spaces as underscores.
     */
    public String id() {
        return "economylite:" + this.configKey();
    }

    /** The key of the currency in {@code currencies.conf}. */
    public String configKey() {
        return this.singular.toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    /** The key for Sponge's currency registry, empty when the name has characters a resource key may not hold. */
    public Optional<ResourceKey> registryKey() {
        final String value = this.configKey();
        if (value.isEmpty() || !value.matches("[a-z0-9_.\\-/]+")) {
            return Optional.empty();
        }
        return Optional.of(ResourceKey.of("economylite", value));
    }

    public String singular() {
        return this.singular;
    }

    public String plural() {
        return this.plural;
    }

    public String symbolText() {
        return this.symbol;
    }

    /** The singular name for exactly one, otherwise the plural one. */
    public String label(final BigDecimal amount) {
        return amount.compareTo(BigDecimal.ONE) == 0 ? this.singular : this.plural;
    }

    @Override
    public Component displayName() {
        return Component.text(this.singular);
    }

    @Override
    public Component pluralDisplayName() {
        return Component.text(this.plural);
    }

    @Override
    public Component symbol() {
        return Component.text(this.symbol);
    }

    @Override
    public Component format(final BigDecimal amount, final int numFractionDigits) {
        return Component.text(formatAmount(amount, numFractionDigits) + " " + this.label(amount));
    }

    @Override
    public int defaultFractionDigits() {
        return this.digits;
    }

    @Override
    public boolean isDefault() {
        return this.defaultCurrency;
    }

    /** An amount as EconomyLite shows it: grouped, with two decimals. */
    public static String formatAmount(final BigDecimal amount) {
        return formatAmount(amount, 2);
    }

    private static String formatAmount(final BigDecimal amount, final int digits) {
        return String.format(Locale.ENGLISH, "%,." + digits + "f", amount.setScale(digits, RoundingMode.HALF_UP));
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LiteCurrency && this.id().equals(((LiteCurrency) other).id());
    }

    @Override
    public int hashCode() {
        return this.id().hashCode();
    }

    @Override
    public String toString() {
        return this.id();
    }
}
