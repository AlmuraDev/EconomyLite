/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite;

import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;

public final class CauseFactory {

    private CauseFactory() {
    }

    /** A cause of EconomyLite itself, with what it is doing. */
    public static Cause create(final String reason) {
        return Cause.of(EventContext.empty(), EconomyLite.getInstance().getPluginContainer(), reason);
    }

    /** A cause of a command: whoever ran it, then EconomyLite. */
    public static Cause command(final Cause commandCause) {
        return Cause.builder()
                .from(commandCause)
                .append(EconomyLite.getInstance().getPluginContainer())
                .build(commandCause.context());
    }
}
