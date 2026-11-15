/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.CommandCause;

import java.util.function.Consumer;

public final class TextUtils {

    private TextUtils() {
    }

    public static Component yesOrNo(final Consumer<CommandCause> yes, final Consumer<CommandCause> no) {
        final Component accept = Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY), Component.text("YES", NamedTextColor.GREEN),
                        Component.text("]", NamedTextColor.DARK_GRAY))
                .hoverEvent(HoverEvent.showText(Component.text("Yes!", NamedTextColor.GREEN)))
                .clickEvent(SpongeComponents.executeCallback(yes))
                .build();
        final Component deny = Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY), Component.text("NO", NamedTextColor.RED),
                        Component.text("]", NamedTextColor.DARK_GRAY))
                .hoverEvent(HoverEvent.showText(Component.text("No!", NamedTextColor.RED)))
                .clickEvent(SpongeComponents.executeCallback(no))
                .build();
        return Component.text().append(accept, Component.space(), deny).build();
    }

    /** Makes a message run a callback when clicked. */
    public static Component onClick(final Component message, final Consumer<CommandCause> callback) {
        return message.clickEvent(SpongeComponents.executeCallback(callback));
    }
}
