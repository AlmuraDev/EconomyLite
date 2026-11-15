/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.api;

import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Interfaces directly with the storage system. Performs no checks, simply does exactly what it is told.
 */
public interface PlayerEconService {

    /**
     * Gets the balance of a player.
     *
     * @param uuid The UUID of the player to get the balance of.
     * @param currency The currency to use.
     * @param cause What is getting the balance.
     * @return The balance of the player, zero if the player has none in the currency.
     */
    BigDecimal getBalance(UUID uuid, Currency currency, Cause cause);

    /**
     * Sets the balance of a player.
     *
     * @param uuid The UUID of the player to set the balance of.
     * @param balance The new balance of the uuid.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the player's balance was changed successfully.
     */
    boolean setBalance(UUID uuid, BigDecimal balance, Currency currency, Cause cause);

    /**
     * Removes currency from a player's balance.
     *
     * @param uuid The UUID of the player to remove currency from.
     * @param amount The amount of currency to remove.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the player's balance was changed successfully.
     */
    default boolean withdraw(UUID uuid, BigDecimal amount, Currency currency, Cause cause) {
        return setBalance(uuid, getBalance(uuid, currency, cause).subtract(amount), currency, cause);
    }

    /**
     * Adds currency to a player's balance.
     *
     * @param uuid The UUID of the player to add currency to.
     * @param amount The amount of currency to add.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the player's balance was changed successfully.
     */
    default boolean deposit(UUID uuid, BigDecimal amount, Currency currency, Cause cause) {
        return setBalance(uuid, getBalance(uuid, currency, cause).add(amount), currency, cause);
    }

    /**
     * Checks if a player exists in the system for the specified currency.
     *
     * @param uuid The UUID of the player to check for.
     * @param currency The currency to check against.
     * @param cause What is checking if the account exists.
     * @return If the player exists or not.
     */
    boolean accountExists(UUID uuid, Currency currency, Cause cause);

    /**
     * Gets every player with a balance in a currency.
     *
     * @param currency The currency.
     * @return The UUIDs of the players.
     */
    List<UUID> getAccounts(Currency currency);

    /**
     * Deletes every balance of a player.
     *
     * @param uuid The UUID of the player.
     * @param cause What is deleting the account.
     * @return If the player had any balance that was deleted.
     */
    boolean deleteAccount(UUID uuid, Cause cause);

    /**
     * Clears a currency from the database.
     *
     * @param currency The currency to clear.
     * @param cause What is clearing the balances.
     */
    void clearCurrency(Currency currency, Cause cause);

    /**
     * Gets the top unique accounts registered in the EconomyLite system, in the current currency.
     *
     * @param start The starting account to get, counting from 1.
     * @param end The ending account to get.
     * @param cause What is getting the accounts.
     * @return The top unique accounts registered in the EconomyLite system.
     */
    List<UniqueAccount> getTopAccounts(int start, int end, Cause cause);

    /**
     * Sets the balance of all players.
     *
     * @param balance The new balance.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the balances were changed successfully.
     */
    boolean setBalanceAll(BigDecimal balance, Currency currency, Cause cause);
}
