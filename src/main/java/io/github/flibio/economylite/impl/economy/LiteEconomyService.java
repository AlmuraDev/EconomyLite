/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy;

import io.github.flibio.economylite.CauseFactory;
import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.impl.economy.account.LiteUniqueAccount;
import io.github.flibio.economylite.impl.economy.account.LiteVirtualAccount;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.AccountDeletionResultType;
import org.spongepowered.api.service.economy.account.AccountDeletionResultTypes;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.account.VirtualAccount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * EconomyLite's {@link EconomyService}. An account exists once it has a balance in the current currency; it is
 * created with the configured default balance.
 */
public final class LiteEconomyService implements EconomyService {

    @Override
    public Currency defaultCurrency() {
        return EconomyLite.getCurrencyService().getCurrentCurrency();
    }

    @Override
    public boolean hasAccount(final UUID uuid) {
        return EconomyLite.getPlayerService().accountExists(uuid, this.defaultCurrency(), CauseFactory.create("Checking account existence"));
    }

    @Override
    public boolean hasAccount(final String identifier) {
        return EconomyLite.getVirtualService().accountExists(identifier, this.defaultCurrency(),
                CauseFactory.create("Checking account existence"));
    }

    @Override
    public Optional<UniqueAccount> findOrCreateAccount(final UUID uuid) {
        final LiteUniqueAccount account = new LiteUniqueAccount(uuid);
        synchronized (EconomyLite.getPlayerService()) {
            if (this.hasAccount(uuid)) {
                return Optional.of(account);
            }
            final Currency currency = this.defaultCurrency();
            if (EconomyLite.getPlayerService().setBalance(uuid, account.defaultBalance(currency), currency, CauseFactory.create("Creating account"))) {
                return Optional.of(account);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findOrCreateAccount(final String identifier) {
        final LiteVirtualAccount account = new LiteVirtualAccount(identifier);
        synchronized (EconomyLite.getVirtualService()) {
            if (this.hasAccount(identifier)) {
                return Optional.of(account);
            }
            final Currency currency = this.defaultCurrency();
            if (EconomyLite.getVirtualService().setBalance(identifier, account.defaultBalance(currency), currency,
                    CauseFactory.create("Creating account"))) {
                return Optional.of(account);
            }
        }
        return Optional.empty();
    }

    @Override
    public Stream<UniqueAccount> streamUniqueAccounts() {
        return this.uniqueAccounts().stream();
    }

    @Override
    public Collection<UniqueAccount> uniqueAccounts() {
        final List<UUID> uuids = EconomyLite.getPlayerService().getAccounts(this.defaultCurrency());
        return uuids.stream().<UniqueAccount>map(LiteUniqueAccount::new).toList();
    }

    @Override
    public Stream<VirtualAccount> streamVirtualAccounts() {
        return this.virtualAccounts().stream();
    }

    @Override
    public Collection<VirtualAccount> virtualAccounts() {
        final List<String> ids = EconomyLite.getVirtualService().getAccounts(this.defaultCurrency());
        return ids.stream().<VirtualAccount>map(LiteVirtualAccount::new).toList();
    }

    @Override
    public AccountDeletionResultType deleteAccount(final UUID uuid) {
        return EconomyLite.getPlayerService().deleteAccount(uuid, CauseFactory.create("Deleting account"))
                ? AccountDeletionResultTypes.SUCCESS.get()
                : AccountDeletionResultTypes.ABSENT.get();
    }

    @Override
    public AccountDeletionResultType deleteAccount(final String identifier) {
        return EconomyLite.getVirtualService().deleteAccount(identifier, CauseFactory.create("Deleting account"))
                ? AccountDeletionResultTypes.SUCCESS.get()
                : AccountDeletionResultTypes.ABSENT.get();
    }
}
