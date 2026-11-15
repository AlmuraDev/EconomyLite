/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy.result;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

public class LiteTransactionResult implements TransactionResult {

    private final Account account;
    private final BigDecimal amount;
    private final Currency currency;
    private final ResultType result;
    private final TransactionType transactionType;

    public LiteTransactionResult(final Account account, final BigDecimal amount, final Currency currency, final ResultType result,
            final TransactionType transactionType) {
        this.account = account;
        this.amount = amount;
        this.currency = currency;
        this.result = result;
        this.transactionType = transactionType;
    }

    @Override
    public Account account() {
        return this.account;
    }

    @Override
    public BigDecimal amount() {
        return this.amount;
    }

    @Override
    public Set<Context> contexts() {
        return Collections.emptySet();
    }

    @Override
    public Currency currency() {
        return this.currency;
    }

    @Override
    public ResultType result() {
        return this.result;
    }

    @Override
    public TransactionType type() {
        return this.transactionType;
    }
}
