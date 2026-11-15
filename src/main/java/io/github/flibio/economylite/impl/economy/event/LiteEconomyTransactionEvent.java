/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy.event;

import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.economy.EconomyTransactionEvent;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

public final class LiteEconomyTransactionEvent extends AbstractEvent implements EconomyTransactionEvent {

    private final TransactionResult result;
    private final Cause cause;

    public LiteEconomyTransactionEvent(final TransactionResult result, final Cause cause) {
        this.result = result;
        this.cause = cause;
    }

    @Override
    public Cause cause() {
        return this.cause;
    }

    @Override
    public TransactionResult transactionResult() {
        return this.result;
    }
}
