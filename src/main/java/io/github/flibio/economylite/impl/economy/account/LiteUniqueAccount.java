/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.impl.economy.account;

import io.github.flibio.economylite.EconomyLite;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public final class LiteUniqueAccount extends LiteAccount implements UniqueAccount {

    private final UUID uuid;

    public LiteUniqueAccount(final UUID uuid) {
        this.uuid = uuid;
    }

    /** The player's name as the server knows it, otherwise the UUID. */
    public String name() {
        return nameOf(this.uuid);
    }

    public static String nameOf(final UUID uuid) {
        try {
            if (Sponge.isServerAvailable()) {
                final Optional<ServerPlayer> online = Sponge.server().player(uuid);
                if (online.isPresent()) {
                    return online.get().name();
                }
                final Optional<String> cached = Sponge.server().gameProfileManager().cache().findById(uuid).flatMap(GameProfile::name);
                if (cached.isPresent()) {
                    return cached.get();
                }
            }
        } catch (final RuntimeException ignored) {
            // Fall back to the UUID
        }
        return uuid.toString();
    }

    @Override
    public Component displayName() {
        return Component.text(this.name());
    }

    @Override
    public BigDecimal defaultBalance(final Currency currency) {
        return EconomyLite.getDefaultPlayerBalance();
    }

    @Override
    protected boolean storedExists(final Currency currency, final Cause cause) {
        return EconomyLite.getPlayerService().accountExists(this.uuid, currency, cause);
    }

    @Override
    protected BigDecimal storedBalance(final Currency currency, final Cause cause) {
        return EconomyLite.getPlayerService().getBalance(this.uuid, currency, cause);
    }

    @Override
    protected boolean storeBalance(final Currency currency, final BigDecimal balance, final Cause cause) {
        return EconomyLite.getPlayerService().setBalance(this.uuid, balance, currency, cause);
    }

    @Override
    protected Object lock() {
        return EconomyLite.getPlayerService();
    }

    @Override
    public String identifier() {
        return this.uuid.toString();
    }

    @Override
    public Optional<String> friendlyIdentifier() {
        return Optional.of(this.name());
    }

    @Override
    public UUID uniqueId() {
        return this.uuid;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LiteUniqueAccount && ((LiteUniqueAccount) other).uuid.equals(this.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }
}
