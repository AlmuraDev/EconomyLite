/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.api;

import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.VirtualAccount;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interfaces directly with the storage system. Performs no checks, simply does exactly what it is told.
 */
public interface VirtualEconService {

    /**
     * Gets the balance of an account.
     *
     * @param id The name of the account to get the balance of.
     * @param currency The currency to use.
     * @param cause What is getting the balance.
     * @return The balance of the account, zero if the account has none in the currency.
     */
    BigDecimal getBalance(String id, Currency currency, Cause cause);

    /**
     * Sets the balance of an account.
     *
     * @param id The name of the account to set the balance of.
     * @param balance The new balance of the id.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the account's balance was changed successfully.
     */
    boolean setBalance(String id, BigDecimal balance, Currency currency, Cause cause);

    /**
     * Removes currency from an account's balance.
     *
     * @param id The name of the account to remove currency from.
     * @param amount The amount of currency to remove.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the account's balance was changed successfully.
     */
    default boolean withdraw(String id, BigDecimal amount, Currency currency, Cause cause) {
        return setBalance(id, getBalance(id, currency, cause).subtract(amount), currency, cause);
    }

    /**
     * Adds currency to an account's balance.
     *
     * @param id The name of the account to add currency to.
     * @param amount The amount of currency to add.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the account's balance was changed successfully.
     */
    default boolean deposit(String id, BigDecimal amount, Currency currency, Cause cause) {
        return setBalance(id, getBalance(id, currency, cause).add(amount), currency, cause);
    }

    /**
     * Checks if an account exists in the system for the specified currency.
     *
     * @param id The name of the account to check for.
     * @param currency The currency to check against.
     * @param cause What is checking if the account exists.
     * @return If the account exists or not.
     */
    boolean accountExists(String id, Currency currency, Cause cause);

    /**
     * Gets every virtual account with a balance in a currency.
     *
     * @param currency The currency.
     * @return The names of the accounts.
     */
    List<String> getAccounts(Currency currency);

    /**
     * Deletes every balance of an account.
     *
     * @param id The name of the account.
     * @param cause What is deleting the account.
     * @return If the account had any balance that was deleted.
     */
    boolean deleteAccount(String id, Cause cause);

    /**
     * Clears a currency from the database.
     *
     * @param currency The currency to clear.
     * @param cause What is clearing the balances.
     */
    void clearCurrency(Currency currency, Cause cause);

    /**
     * Gets the top virtual accounts registered in the EconomyLite system, in the current currency.
     *
     * @param start The starting account to get, counting from 1.
     * @param end The ending account to get.
     * @param cause What is getting the accounts.
     * @return The top virtual accounts registered in the EconomyLite system.
     */
    List<VirtualAccount> getTopAccounts(int start, int end, Cause cause);
}
