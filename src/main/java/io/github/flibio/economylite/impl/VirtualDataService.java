/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.api.VirtualEconService;
import io.github.flibio.economylite.storage.Database;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.VirtualAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Virtual account balances, in the {@code economylitevirts} table.
 */
public final class VirtualDataService implements VirtualEconService {

    private final Database database;
    private final Logger logger;
    private final boolean debug;

    public VirtualDataService(final Database database, final Logger logger, final boolean debug) {
        this.database = database;
        this.logger = logger;
        this.debug = debug;
    }

    private static String id(final Currency currency) {
        return EconomyLite.getCurrencyService().getId(currency);
    }

    @Override
    public BigDecimal getBalance(final String id, final Currency currency, final Cause cause) {
        final BigDecimal result = this.database.queryDecimal("SELECT balance FROM economylitevirts WHERE id = ? AND currency = ?", id,
                id(currency)).orElse(BigDecimal.ZERO);
        this.debug("Balance of '" + id + "' with '" + id(currency) + "' = " + result.toPlainString(), cause);
        return result;
    }

    @Override
    public synchronized boolean setBalance(final String id, final BigDecimal balance, final Currency currency, final Cause cause) {
        final boolean result;
        if (this.accountExists(id, currency, cause)) {
            result = this.database.update("UPDATE economylitevirts SET balance = ? WHERE id = ? AND currency = ?", balance, id, id(currency));
        } else {
            result = this.database.update("INSERT INTO economylitevirts (id, balance, currency) VALUES (?, ?, ?)", id, balance, id(currency));
        }
        this.debug("Setting balance of '" + id + "' to '" + balance.toPlainString() + "' with '" + id(currency) + "' = " + result, cause);
        return result;
    }

    @Override
    public synchronized boolean withdraw(final String id, final BigDecimal amount, final Currency currency, final Cause cause) {
        return this.setBalance(id, this.getBalance(id, currency, cause).subtract(amount), currency, cause);
    }

    @Override
    public synchronized boolean deposit(final String id, final BigDecimal amount, final Currency currency, final Cause cause) {
        return this.setBalance(id, this.getBalance(id, currency, cause).add(amount), currency, cause);
    }

    @Override
    public boolean accountExists(final String id, final Currency currency, final Cause cause) {
        return this.database.queryExists("SELECT id FROM economylitevirts WHERE id = ? AND currency = ?", id, id(currency)).orElse(false);
    }

    @Override
    public List<String> getAccounts(final Currency currency) {
        return this.database.queryStrings("SELECT DISTINCT id FROM economylitevirts WHERE currency = ?", id(currency));
    }

    @Override
    public boolean deleteAccount(final String id, final Cause cause) {
        final boolean existed = this.database.queryExists("SELECT id FROM economylitevirts WHERE id = ?", id).orElse(false);
        final boolean result = existed && this.database.update("DELETE FROM economylitevirts WHERE id = ?", id);
        this.debug("Deleting account '" + id + "' = " + result, cause);
        return result;
    }

    @Override
    public void clearCurrency(final Currency currency, final Cause cause) {
        final boolean result = this.database.update("DELETE FROM economylitevirts WHERE currency = ?", id(currency));
        this.debug("Clearing currency '" + id(currency) + "' = " + result, cause);
    }

    @Override
    public List<VirtualAccount> getTopAccounts(final int start, final int end, final Cause cause) {
        final Currency currency = EconomyLite.getCurrencyService().getCurrentCurrency();
        final int offset = Math.max(0, start - 1);
        final int limit = Math.max(0, end - offset);
        final List<VirtualAccount> accounts = new ArrayList<>();
        for (final String id : this.database.queryStrings(
                "SELECT id FROM economylitevirts WHERE currency = ? ORDER BY balance DESC LIMIT ? OFFSET ?", id(currency), limit, offset)) {
            final Optional<Account> account = EconomyLite.getEconomyService().findOrCreateAccount(id);
            if (account.isPresent() && account.get() instanceof VirtualAccount virtual) {
                accounts.add(virtual);
            }
        }
        this.debug("Getting top accounts " + start + "-" + end, cause);
        return accounts;
    }

    private void debug(final String message, final Cause cause) {
        if (this.debug) {
            this.logger.debug("virtcommon: {} - {}", message, cause);
        }
    }
}
