/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy.result;

import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionTypes;
import org.spongepowered.api.service.economy.transaction.TransferResult;

import java.math.BigDecimal;

public final class LiteTransferResult extends LiteTransactionResult implements TransferResult {

    private final Account accountTo;

    public LiteTransferResult(final Account account, final BigDecimal amount, final Currency currency, final ResultType result,
            final Account accountTo) {
        super(account, amount, currency, result, TransactionTypes.TRANSFER.get());
        this.accountTo = accountTo;
    }

    @Override
    public Account accountTo() {
        return this.accountTo;
    }
}
