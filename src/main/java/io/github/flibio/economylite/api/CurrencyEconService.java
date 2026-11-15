/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.api;

import org.spongepowered.api.service.economy.Currency;

import java.util.Set;

public interface CurrencyEconService {

    /**
     * Gets all currencies.
     *
     * @return A set of all currencies.
     */
    Set<Currency> getCurrencies();

    /**
     * Adds a currency to the currencies.
     *
     * @param currency The currency to add.
     */
    void addCurrency(Currency currency);

    /**
     * Gets the default currency, the one EconomyLite always has.
     *
     * @return The default currency.
     */
    Currency getDefaultCurrency();

    /**
     * Sets the currency in use by the server.
     *
     * @param currency The currency in use.
     */
    void setCurrentCurrency(Currency currency);

    /**
     * Deletes a currency and every balance in it.
     *
     * @param currency The currency to delete.
     */
    void deleteCurrency(Currency currency);

    /**
     * Gets the currency in use by the server.
     *
     * @return The currency in use.
     */
    Currency getCurrentCurrency();

    /**
     * Gets the id balances of a currency are stored under, such as {@code economylite:coin}.
     *
     * @param currency The currency.
     * @return The id of the currency.
     */
    String getId(Currency currency);
}
