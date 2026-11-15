/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy.account;

import io.github.flibio.economylite.EconomyLite;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.VirtualAccount;

import java.math.BigDecimal;

public final class LiteVirtualAccount extends LiteAccount implements VirtualAccount {

    private final String name;

    public LiteVirtualAccount(final String name) {
        this.name = name;
    }

    @Override
    public Component displayName() {
        return Component.text(this.name);
    }

    @Override
    public BigDecimal defaultBalance(final Currency currency) {
        return EconomyLite.getDefaultVirtualBalance();
    }

    @Override
    protected boolean storedExists(final Currency currency, final Cause cause) {
        return EconomyLite.getVirtualService().accountExists(this.name, currency, cause);
    }

    @Override
    protected BigDecimal storedBalance(final Currency currency, final Cause cause) {
        return EconomyLite.getVirtualService().getBalance(this.name, currency, cause);
    }

    @Override
    protected boolean storeBalance(final Currency currency, final BigDecimal balance, final Cause cause) {
        return EconomyLite.getVirtualService().setBalance(this.name, balance, currency, cause);
    }

    @Override
    protected Object lock() {
        return EconomyLite.getVirtualService();
    }

    @Override
    public String identifier() {
        return this.name;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LiteVirtualAccount && ((LiteVirtualAccount) other).name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
