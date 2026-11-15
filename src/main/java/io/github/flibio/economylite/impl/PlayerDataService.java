/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.api.PlayerEconService;
import io.github.flibio.economylite.storage.Database;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player balances, in the {@code economyliteplayers} table.
 */
public final class PlayerDataService implements PlayerEconService {

    private final Database database;
    private final Logger logger;
    private final boolean debug;

    public PlayerDataService(final Database database, final Logger logger, final boolean debug) {
        this.database = database;
        this.logger = logger;
        this.debug = debug;
    }

    private static String id(final Currency currency) {
        return EconomyLite.getCurrencyService().getId(currency);
    }

    @Override
    public BigDecimal getBalance(final UUID uuid, final Currency currency, final Cause cause) {
        final BigDecimal result = this.database.queryDecimal("SELECT balance FROM economyliteplayers WHERE uuid = ? AND currency = ?",
                uuid.toString(), id(currency)).orElse(BigDecimal.ZERO);
        this.debug("Balance of '" + uuid + "' with '" + id(currency) + "' = " + result.toPlainString(), cause);
        return result;
    }

    @Override
    public synchronized boolean setBalance(final UUID uuid, final BigDecimal balance, final Currency currency, final Cause cause) {
        final boolean result;
        if (this.accountExists(uuid, currency, cause)) {
            result = this.database.update("UPDATE economyliteplayers SET balance = ? WHERE uuid = ? AND currency = ?", balance, uuid.toString(),
                    id(currency));
        } else {
            result = this.database.update("INSERT INTO economyliteplayers (uuid, balance, currency) VALUES (?, ?, ?)", uuid.toString(), balance,
                    id(currency));
        }
        this.debug("Setting balance of '" + uuid + "' to '" + balance.toPlainString() + "' with '" + id(currency) + "' = " + result, cause);
        return result;
    }

    @Override
    public synchronized boolean withdraw(final UUID uuid, final BigDecimal amount, final Currency currency, final Cause cause) {
        return this.setBalance(uuid, this.getBalance(uuid, currency, cause).subtract(amount), currency, cause);
    }

    @Override
    public synchronized boolean deposit(final UUID uuid, final BigDecimal amount, final Currency currency, final Cause cause) {
        return this.setBalance(uuid, this.getBalance(uuid, currency, cause).add(amount), currency, cause);
    }

    @Override
    public boolean accountExists(final UUID uuid, final Currency currency, final Cause cause) {
        return this.database.queryExists("SELECT uuid FROM economyliteplayers WHERE uuid = ? AND currency = ?", uuid.toString(), id(currency))
                .orElse(false);
    }

    @Override
    public List<UUID> getAccounts(final Currency currency) {
        final List<UUID> accounts = new ArrayList<>();
        for (final String uuid : this.database.queryStrings("SELECT DISTINCT uuid FROM economyliteplayers WHERE currency = ?", id(currency))) {
            try {
                accounts.add(UUID.fromString(uuid));
            } catch (final IllegalArgumentException e) {
                this.logger.warn("Skipping the player account '{}', it is not a UUID", uuid);
            }
        }
        return accounts;
    }

    @Override
    public boolean deleteAccount(final UUID uuid, final Cause cause) {
        final boolean existed = this.database.queryExists("SELECT uuid FROM economyliteplayers WHERE uuid = ?", uuid.toString()).orElse(false);
        final boolean result = existed && this.database.update("DELETE FROM economyliteplayers WHERE uuid = ?", uuid.toString());
        this.debug("Deleting account '" + uuid + "' = " + result, cause);
        return result;
    }

    @Override
    public void clearCurrency(final Currency currency, final Cause cause) {
        final boolean result = this.database.update("DELETE FROM economyliteplayers WHERE currency = ?", id(currency));
        this.debug("Clearing currency '" + id(currency) + "' = " + result, cause);
    }

    @Override
    public List<UniqueAccount> getTopAccounts(final int start, final int end, final Cause cause) {
        final Currency currency = EconomyLite.getCurrencyService().getCurrentCurrency();
        final int offset = Math.max(0, start - 1);
        final int limit = Math.max(0, end - offset);
        final List<UniqueAccount> accounts = new ArrayList<>();
        for (final String uuid : this.database.queryStrings(
                "SELECT uuid FROM economyliteplayers WHERE currency = ? ORDER BY balance DESC LIMIT ? OFFSET ?", id(currency), limit, offset)) {
            try {
                EconomyLite.getEconomyService().findOrCreateAccount(UUID.fromString(uuid)).ifPresent(accounts::add);
            } catch (final IllegalArgumentException e) {
                this.logger.warn("Skipping the player account '{}', it is not a UUID", uuid);
            }
        }
        this.debug("Getting top accounts " + start + "-" + end, cause);
        return accounts;
    }

    @Override
    public boolean setBalanceAll(final BigDecimal balance, final Currency currency, final Cause cause) {
        final boolean result = this.database.update("UPDATE economyliteplayers SET balance = ? WHERE currency = ?", balance, id(currency));
        this.debug("Setting balance of ALL to '" + balance.toPlainString() + "' with '" + id(currency) + "' = " + result, cause);
        return result;
    }

    private void debug(final String message, final Cause cause) {
        if (this.debug) {
            this.logger.debug("playercommon: {} - {}", message, cause);
        }
    }
}
