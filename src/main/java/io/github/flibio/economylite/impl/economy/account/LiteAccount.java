/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy.account;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.impl.economy.event.LiteEconomyTransactionEvent;
import io.github.flibio.economylite.impl.economy.result.LiteTransactionResult;
import io.github.flibio.economylite.impl.economy.result.LiteTransferResult;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import org.spongepowered.api.service.economy.transaction.TransactionTypes;
import org.spongepowered.api.service.economy.transaction.TransferResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * What player and virtual accounts share: balances live in EconomyLite's database, and may lie between zero and
 * {@value #MAX_BALANCE}, which the database's {@code DECIMAL(11,2)} can hold. EconomyLite ignores contexts.
 */
public abstract class LiteAccount implements Account {

    public static final long MAX_BALANCE = 999_999_999L;
    private static final BigDecimal MAX = BigDecimal.valueOf(MAX_BALANCE);

    protected abstract boolean storedExists(Currency currency, Cause cause);

    protected abstract BigDecimal storedBalance(Currency currency, Cause cause);

    protected abstract boolean storeBalance(Currency currency, BigDecimal balance, Cause cause);

    /** Locks one account's read, change and write together. */
    protected abstract Object lock();

    private static Cause cause(final Set<Context> contexts) {
        return Cause.of(EventContext.empty(), EconomyLite.getInstance().getPluginContainer());
    }

    private static boolean outOfBounds(final BigDecimal balance) {
        return balance.signum() < 0 || balance.compareTo(MAX) > 0;
    }

    @Override
    public boolean hasBalance(final Currency currency, final Set<Context> contexts) {
        return this.hasBalance(currency, cause(contexts));
    }

    @Override
    public boolean hasBalance(final Currency currency, final Cause cause) {
        return this.storedExists(currency, cause);
    }

    @Override
    public BigDecimal balance(final Currency currency, final Set<Context> contexts) {
        return this.balance(currency, cause(contexts));
    }

    @Override
    public BigDecimal balance(final Currency currency, final Cause cause) {
        return this.storedBalance(currency, cause);
    }

    @Override
    public Map<Currency, BigDecimal> balances(final Set<Context> contexts) {
        return this.balances(cause(contexts));
    }

    @Override
    public Map<Currency, BigDecimal> balances(final Cause cause) {
        final Map<Currency, BigDecimal> balances = new HashMap<>();
        for (final Currency currency : EconomyLite.getCurrencyService().getCurrencies()) {
            if (this.storedExists(currency, cause)) {
                balances.put(currency, this.storedBalance(currency, cause));
            }
        }
        return balances;
    }

    @Override
    public TransactionResult setBalance(final Currency currency, final BigDecimal amount, final Set<Context> contexts) {
        return this.setBalance(currency, amount, cause(contexts));
    }

    @Override
    public TransactionResult setBalance(final Currency currency, final BigDecimal amount, final Cause cause) {
        if (outOfBounds(amount)) {
            return this.post(amount, currency, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.DEPOSIT.get(), cause);
        }
        final boolean done;
        synchronized (this.lock()) {
            done = this.storeBalance(currency, amount, cause);
        }
        return this.post(amount, currency, done ? ResultType.SUCCESS : ResultType.FAILED, TransactionTypes.DEPOSIT.get(), cause);
    }

    @Override
    public Map<Currency, TransactionResult> resetBalances(final Set<Context> contexts) {
        return this.resetBalances(cause(contexts));
    }

    @Override
    public Map<Currency, TransactionResult> resetBalances(final Cause cause) {
        final Map<Currency, TransactionResult> results = new HashMap<>();
        for (final Currency currency : EconomyLite.getCurrencyService().getCurrencies()) {
            if (this.storedExists(currency, cause)) {
                results.put(currency, this.resetBalance(currency, cause));
            }
        }
        return results;
    }

    @Override
    public TransactionResult resetBalance(final Currency currency, final Set<Context> contexts) {
        return this.resetBalance(currency, cause(contexts));
    }

    @Override
    public TransactionResult resetBalance(final Currency currency, final Cause cause) {
        final BigDecimal balance = this.defaultBalance(currency);
        final boolean done;
        synchronized (this.lock()) {
            done = this.storeBalance(currency, balance, cause);
        }
        return this.post(balance, currency, done ? ResultType.SUCCESS : ResultType.FAILED, TransactionTypes.WITHDRAW.get(), cause);
    }

    @Override
    public TransactionResult deposit(final Currency currency, final BigDecimal amount, final Set<Context> contexts) {
        return this.deposit(currency, amount, cause(contexts));
    }

    @Override
    public TransactionResult deposit(final Currency currency, final BigDecimal amount, final Cause cause) {
        final ResultType result;
        synchronized (this.lock()) {
            final BigDecimal balance = this.storedBalance(currency, cause).add(amount);
            if (outOfBounds(balance)) {
                result = ResultType.ACCOUNT_NO_SPACE;
            } else {
                result = this.storeBalance(currency, balance, cause) ? ResultType.SUCCESS : ResultType.FAILED;
            }
        }
        return this.post(amount, currency, result, TransactionTypes.DEPOSIT.get(), cause);
    }

    @Override
    public TransactionResult withdraw(final Currency currency, final BigDecimal amount, final Set<Context> contexts) {
        return this.withdraw(currency, amount, cause(contexts));
    }

    @Override
    public TransactionResult withdraw(final Currency currency, final BigDecimal amount, final Cause cause) {
        final ResultType result;
        synchronized (this.lock()) {
            final BigDecimal balance = this.storedBalance(currency, cause).subtract(amount);
            if (balance.signum() < 0) {
                result = ResultType.ACCOUNT_NO_FUNDS;
            } else if (balance.compareTo(MAX) > 0) {
                result = ResultType.ACCOUNT_NO_SPACE;
            } else {
                result = this.storeBalance(currency, balance, cause) ? ResultType.SUCCESS : ResultType.FAILED;
            }
        }
        return this.post(amount, currency, result, TransactionTypes.WITHDRAW.get(), cause);
    }

    @Override
    public TransferResult transfer(final Account to, final Currency currency, final BigDecimal amount, final Set<Context> contexts) {
        return this.transfer(to, currency, amount, cause(contexts));
    }

    @Override
    public TransferResult transfer(final Account to, final Currency currency, final BigDecimal amount, final Cause cause) {
        if (outOfBounds(to.balance(currency, cause).add(amount))) {
            return this.postTransfer(amount, currency, ResultType.ACCOUNT_NO_SPACE, to, cause);
        }
        if (amount.compareTo(this.balance(currency, cause)) > 0) {
            return this.postTransfer(amount, currency, ResultType.ACCOUNT_NO_FUNDS, to, cause);
        }
        if (this.withdraw(currency, amount, cause).result() != ResultType.SUCCESS) {
            return this.postTransfer(amount, currency, ResultType.FAILED, to, cause);
        }
        if (to.deposit(currency, amount, cause).result() != ResultType.SUCCESS) {
            // Give the money back rather than lose it
            this.deposit(currency, amount, cause);
            return this.postTransfer(amount, currency, ResultType.FAILED, to, cause);
        }
        return this.postTransfer(amount, currency, ResultType.SUCCESS, to, cause);
    }

    private TransactionResult post(final BigDecimal amount, final Currency currency, final ResultType resultType, final TransactionType type,
            final Cause cause) {
        final TransactionResult result = new LiteTransactionResult(this, amount, currency, resultType, type);
        Sponge.eventManager().post(new LiteEconomyTransactionEvent(result, cause));
        return result;
    }

    private TransferResult postTransfer(final BigDecimal amount, final Currency currency, final ResultType resultType, final Account to,
            final Cause cause) {
        final TransferResult result = new LiteTransferResult(this, amount, currency, resultType, to);
        Sponge.eventManager().post(new LiteEconomyTransactionEvent(result, cause));
        return result;
    }
}
