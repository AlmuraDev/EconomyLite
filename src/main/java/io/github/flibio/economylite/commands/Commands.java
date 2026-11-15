/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.commands;

import io.github.flibio.economylite.CauseFactory;
import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.TextUtils;
import io.github.flibio.economylite.api.CurrencyEconService;
import io.github.flibio.economylite.config.Messages;
import io.github.flibio.economylite.impl.economy.LiteCurrency;
import io.github.flibio.economylite.impl.economy.account.LiteUniqueAccount;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.plugin.PluginContainer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EconomyLite's commands, with the names, aliases, permissions and messages of EconomyLite 2.x.
 */
public final class Commands {

    private static final int BALTOP_PAGE = 5;

    private Commands() {
    }

    public static void register(final RegisterCommandEvent<Command.Parameterized> event, final PluginContainer container) {
        event.register(container, balance(), "balance", "money", "bal");
        event.register(container, balTop(), "baltop");
        event.register(container, pay(), "pay");
        event.register(container, econ(), "economy", "econ", "eco");
        event.register(container, currency(), "currency", "cur");
        event.register(container, virtualBalance(), "vbalance", "vbal");
        event.register(container, virtualEcon(), "veconomy", "vecon", "veco");
        event.register(container, virtualPay(), "vpay");
        event.register(container, payVirtual(), "payvirtual", "payv");
    }

    // Shared bits

    private static Messages messages() {
        return EconomyLite.getMessages();
    }

    private static CurrencyEconService currencies() {
        return EconomyLite.getCurrencyService();
    }

    private static LiteCurrency current() {
        return (LiteCurrency) currencies().getCurrentCurrency();
    }

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static Parameter.Value<UUID> player(final String key) {
        return Parameter.user().key(key).build();
    }

    private static Parameter.Value<BigDecimal> amount(final String key) {
        return Parameter.bigDecimal().key(key).build();
    }

    private static Parameter.Value<String> account(final String key) {
        return Parameter.string().key(key).build();
    }

    /** Amounts are kept to two decimals, as the database keeps them. */
    private static BigDecimal money(final BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String amountAndLabel(final BigDecimal amount, final LiteCurrency currency) {
        return LiteCurrency.formatAmount(amount) + " " + currency.label(amount);
    }

    private static CommandResult error(final CommandContext context) {
        context.sendMessage(messages().get("command.error"));
        return CommandResult.success();
    }

    private static Optional<ServerPlayer> playerSource(final CommandContext context) {
        if (context.cause().root() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        context.sendMessage(messages().get("command.invalidsource", "sourcetype", "player"));
        return Optional.empty();
    }

    private static void notifyIfOnline(final UUID target) {
        if (EconomyLite.getConfig().node("notify-on-admin-commands").getBoolean(false)) {
            Sponge.server().player(target).ifPresent(p -> p.sendMessage(messages().get("command.econ.notify")));
        }
    }

    // /balance [player]

    private static Command.Parameterized balance() {
        final Parameter.Value<UUID> player = Parameter.user().key("player").optional().build();
        return Command.builder()
                .permission("economylite.balance")
                .addParameter(player)
                .executor(context -> {
                    final LiteCurrency currency = current();
                    final Optional<UUID> target = context.one(player);
                    if (target.isPresent()) {
                        if (!context.hasPermission("economylite.admin.balanceothers")) {
                            context.sendMessage(messages().get("command.noperm"));
                            return CommandResult.success();
                        }
                        final Optional<UniqueAccount> account = EconomyLite.getEconomyService().findOrCreateAccount(target.get());
                        if (account.isEmpty()) {
                            return error(context);
                        }
                        final BigDecimal bal = account.get().balance(currency);
                        context.sendMessage(messages().get("command.balanceother", "player", LiteUniqueAccount.nameOf(target.get()), "balance",
                                LiteCurrency.formatAmount(bal), "label", currency.label(bal)));
                        return CommandResult.success();
                    }
                    final Optional<ServerPlayer> source = playerSource(context);
                    if (source.isEmpty()) {
                        return CommandResult.success();
                    }
                    final Optional<UniqueAccount> account = EconomyLite.getEconomyService().findOrCreateAccount(source.get().uniqueId());
                    if (account.isEmpty()) {
                        return error(context);
                    }
                    final BigDecimal bal = account.get().balance(currency);
                    context.sendMessage(messages().get("command.balance", "balance", LiteCurrency.formatAmount(bal), "label", currency.label(bal)));
                    return CommandResult.success();
                })
                .build();
    }

    // /baltop [page]

    private static Command.Parameterized balTop() {
        final Parameter.Value<Integer> page = Parameter.integerNumber().key("page").optional().build();
        return Command.builder()
                .permission("economylite.baltop")
                .addParameter(page)
                .executor(context -> {
                    final int pageNumber = context.one(page).orElse(1);
                    if (pageNumber < 1) {
                        context.sendMessage(messages().get("command.baltop.invalidpage"));
                        return CommandResult.success();
                    }
                    final LiteCurrency currency = current();
                    final int start = (pageNumber - 1) * BALTOP_PAGE + 1;
                    // One more than a page tells whether there is a next page
                    final List<UniqueAccount> accounts =
                            EconomyLite.getPlayerService().getTopAccounts(start, start + BALTOP_PAGE, CauseFactory.create("Baltop command."));
                    if (accounts.isEmpty()) {
                        context.sendMessage(messages().get("command.baltop.nodata"));
                        return CommandResult.success();
                    }
                    context.sendMessage(messages().get("command.baltop.head", "page", String.valueOf(pageNumber)));
                    for (int i = 0; i < Math.min(BALTOP_PAGE, accounts.size()); i++) {
                        final UniqueAccount account = accounts.get(i);
                        final BigDecimal bal = account.balance(currency);
                        context.sendMessage(messages().get("command.baltop.data", "position", Integer.toString(start + i), "name",
                                plain(account.displayName()), "balance", LiteCurrency.formatAmount(bal), "label", currency.label(bal)));
                    }
                    Component navigation = Component.empty();
                    if (pageNumber > 1) {
                        navigation = navigation.append(messages().get("command.baltop.navigation", "button", "<-")
                                .clickEvent(ClickEvent.runCommand("/baltop " + (pageNumber - 1)))
                                .hoverEvent(HoverEvent.showText(Component.text("BACK", NamedTextColor.GREEN))))
                                .append(Component.space());
                    }
                    navigation = navigation.append(messages().get("command.baltop.navigation", "button", String.valueOf(pageNumber)));
                    if (accounts.size() > BALTOP_PAGE) {
                        navigation = navigation.append(Component.space())
                                .append(messages().get("command.baltop.navigation", "button", "->")
                                        .clickEvent(ClickEvent.runCommand("/baltop " + (pageNumber + 1)))
                                        .hoverEvent(HoverEvent.showText(Component.text("NEXT", NamedTextColor.GREEN))));
                    }
                    context.sendMessage(navigation);
                    return CommandResult.success();
                })
                .build();
    }

    // /pay <player> <amount>

    private static Command.Parameterized pay() {
        final Parameter.Value<UUID> player = player("player");
        final Parameter.Value<BigDecimal> amount = amount("amount");
        return Command.builder()
                .permission("economylite.pay")
                .addParameters(player, amount)
                .executor(context -> {
                    final Optional<ServerPlayer> source = playerSource(context);
                    if (source.isEmpty()) {
                        return CommandResult.success();
                    }
                    final BigDecimal value = money(context.requireOne(amount));
                    if (value.signum() <= 0) {
                        context.sendMessage(messages().get("command.pay.invalid"));
                        return CommandResult.success();
                    }
                    final UUID target = context.requireOne(player);
                    final String targetName = LiteUniqueAccount.nameOf(target);
                    final Cause cause = CauseFactory.command(context.cause().cause());
                    final boolean online = Sponge.server().player(target).isPresent();
                    if (online || !EconomyLite.getConfig().node("confirm-offline-payments").getBoolean(false)) {
                        payPlayer(source.get(), target, targetName, value, cause);
                    } else {
                        context.sendMessage(messages().get("command.pay.confirm", "player", targetName));
                        context.sendMessage(TextUtils.yesOrNo(
                                yes -> payPlayer(source.get(), target, targetName, value, cause),
                                no -> source.get().sendMessage(messages().get("command.pay.confirmno", "player", targetName))));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static void payPlayer(final ServerPlayer source, final UUID target, final String targetName, final BigDecimal amount,
            final Cause cause) {
        if (target.equals(source.uniqueId())) {
            source.sendMessage(messages().get("command.pay.notyou"));
            return;
        }
        final Optional<UniqueAccount> from = EconomyLite.getEconomyService().findOrCreateAccount(source.uniqueId());
        final Optional<UniqueAccount> to = EconomyLite.getEconomyService().findOrCreateAccount(target);
        if (from.isEmpty() || to.isEmpty()) {
            source.sendMessage(messages().get("command.error"));
            return;
        }
        final LiteCurrency currency = current();
        if (from.get().transfer(to.get(), currency, amount, cause).result() == ResultType.SUCCESS) {
            source.sendMessage(messages().get("command.pay.success", "target", targetName, "amountandlabel", amountAndLabel(amount, currency)));
            Sponge.server().player(target).ifPresent(p -> p.sendMessage(messages().get("command.pay.target", "amountandlabel",
                    amountAndLabel(amount, currency), "sender", source.name())));
        } else {
            source.sendMessage(messages().get("command.pay.failed", "target", targetName));
        }
    }

    // /econ add | set | remove <player> <amount>, /econ setall <amount>

    private enum Change {
        ADD("add", "addsuccess", "addfail"),
        SET("set", "setsuccess", "setfail"),
        REMOVE("remove", "removesuccess", "removefail");

        final String name;
        final String success;
        final String fail;

        Change(final String name, final String success, final String fail) {
            this.name = name;
            this.success = "command.econ." + success;
            this.fail = "command.econ." + fail;
        }

        boolean apply(final Account account, final Currency currency, final BigDecimal amount, final Cause cause) {
            final TransactionResult result = switch (this) {
                case ADD -> account.deposit(currency, amount, cause);
                case SET -> account.setBalance(currency, amount, cause);
                case REMOVE -> account.withdraw(currency, amount, cause);
            };
            return result.result() == ResultType.SUCCESS;
        }
    }

    private static Command.Parameterized econ() {
        final Command.Builder builder = Command.builder()
                .permission("economylite.admin.econ")
                .executor(context -> {
                    context.sendMessage(messages().get("command.usage", "command", "/econ", "subcommands", "add | set | remove <player> <amount>"));
                    context.sendMessage(messages().get("command.usage", "command", "/econ", "subcommands", "setall <amount>"));
                    return CommandResult.success();
                });
        for (final Change change : Change.values()) {
            builder.addChild(econChange(change), change.name);
        }
        builder.addChild(econSetAll(), "setall");
        return builder.build();
    }

    private static Command.Parameterized econChange(final Change change) {
        final Parameter.Value<UUID> player = player("player");
        final Parameter.Value<BigDecimal> amount = amount(change == Change.SET ? "balance" : "amount");
        return Command.builder()
                .permission("economylite.admin.econ." + change.name)
                .addParameters(player, amount)
                .executor(context -> {
                    final UUID target = context.requireOne(player);
                    final String targetName = LiteUniqueAccount.nameOf(target);
                    final Optional<UniqueAccount> account = EconomyLite.getEconomyService().findOrCreateAccount(target);
                    if (account.isEmpty()) {
                        return error(context);
                    }
                    if (change.apply(account.get(), current(), money(context.requireOne(amount)), CauseFactory.command(context.cause().cause()))) {
                        context.sendMessage(messages().get(change.success, "name", targetName));
                        notifyIfOnline(target);
                    } else {
                        context.sendMessage(messages().get(change.fail, "name", targetName));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized econSetAll() {
        final Parameter.Value<BigDecimal> balance = amount("balance");
        return Command.builder()
                .permission("economylite.admin.econ.setall")
                .addParameter(balance)
                .executor(context -> {
                    final BigDecimal value = money(context.requireOne(balance));
                    final String targetName = "all players";
                    if (value.signum() >= 0 && EconomyLite.getPlayerService().setBalanceAll(value, current(),
                            CauseFactory.command(context.cause().cause()))) {
                        context.sendMessage(messages().get("command.econ.setsuccess", "name", targetName));
                    } else {
                        context.sendMessage(messages().get("command.econ.setfail", "name", targetName));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    // /currency set | create | delete

    private static Optional<Currency> currencyNamed(final String name) {
        return currencies().getCurrencies().stream().filter(c -> plain(c.displayName()).equalsIgnoreCase(name)).findFirst();
    }

    private static Command.Parameterized currency() {
        return Command.builder()
                .permission("economylite.admin.currency")
                .executor(context -> {
                    context.sendMessage(messages().get("command.currency.current", "currency", current().singular()));
                    context.sendMessage(messages().get("command.usage", "command", "/currency", "subcommands", "set | create | delete"));
                    return CommandResult.success();
                })
                .addChild(currencySet(), "set")
                .addChild(currencyCreate(), "create")
                .addChild(currencyDelete(), "delete")
                .build();
    }

    private static Command.Parameterized currencySet() {
        final Parameter.Value<String> name = Parameter.remainingJoinedStrings().key("currency").optional().build();
        return Command.builder()
                .permission("economylite.admin.currency.set")
                .addParameter(name)
                .executor(context -> {
                    final Optional<String> wanted = context.one(name);
                    if (wanted.isPresent()) {
                        final Optional<Currency> found = currencyNamed(wanted.get());
                        if (found.isPresent()) {
                            currencies().setCurrentCurrency(found.get());
                            context.sendMessage(messages().get("command.currency.changed", "currency", plain(found.get().displayName())));
                        } else {
                            context.sendMessage(messages().get("command.currency.invalid"));
                        }
                        return CommandResult.success();
                    }
                    context.sendMessage(messages().get("command.currency.current", "currency", current().singular()));
                    context.sendMessage(messages().get("command.currency.selectnew"));
                    for (final Currency currency : currencies().getCurrencies()) {
                        final String currencyName = plain(currency.displayName());
                        context.sendMessage(TextUtils.onClick(currency.displayName(), c -> c.sendMessage(TextUtils.onClick(
                                messages().get("command.currency.confirm", "currency", currencyName), c2 -> {
                                    currencies().setCurrentCurrency(currency);
                                    c2.sendMessage(messages().get("command.currency.changed", "currency", currencyName));
                                }))));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized currencyCreate() {
        final Parameter.Value<String> singular = Parameter.string().key("singular").build();
        final Parameter.Value<String> plural = Parameter.string().key("plural").build();
        final Parameter.Value<String> symbol = Parameter.string().key("symbol").build();
        return Command.builder()
                .permission("economylite.admin.currency.create")
                .addParameters(singular, plural, symbol)
                .executor(context -> {
                    final LiteCurrency currency =
                            new LiteCurrency(context.requireOne(singular), context.requireOne(plural), context.requireOne(symbol), false, 2);
                    final boolean exists = currencies().getCurrencies().stream()
                            .anyMatch(c -> plain(c.displayName()).equalsIgnoreCase(currency.singular()) || c.equals(currency));
                    if (exists) {
                        context.sendMessage(messages().get("command.currency.exists"));
                    } else {
                        currencies().addCurrency(currency);
                        context.sendMessage(messages().get("command.currency.created", "currency", currency.singular()));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized currencyDelete() {
        final Parameter.Value<String> name = Parameter.remainingJoinedStrings().key("currency").optional().build();
        return Command.builder()
                .permission("economylite.admin.currency.delete")
                .addParameter(name)
                .executor(context -> {
                    final Optional<String> wanted = context.one(name);
                    if (wanted.isPresent()) {
                        final Optional<Currency> found = currencyNamed(wanted.get());
                        if (found.isEmpty()) {
                            context.sendMessage(messages().get("command.currency.invalid"));
                        } else if (found.get().equals(currencies().getDefaultCurrency())) {
                            context.sendMessage(messages().get("command.currency.deletedefault"));
                        } else {
                            deleteCurrency(found.get());
                            context.sendMessage(messages().get("command.currency.deleted", "currency", plain(found.get().displayName())));
                        }
                        return CommandResult.success();
                    }
                    context.sendMessage(messages().get("command.currency.delete"));
                    for (final Currency currency : currencies().getCurrencies()) {
                        if (currency.equals(currencies().getDefaultCurrency())) {
                            continue;
                        }
                        final String currencyName = plain(currency.displayName());
                        context.sendMessage(TextUtils.onClick(currency.displayName(), c -> c.sendMessage(TextUtils.onClick(
                                messages().get("command.currency.deleteconfirm", "currency", currencyName), c2 -> {
                                    deleteCurrency(currency);
                                    c2.sendMessage(messages().get("command.currency.deleted", "currency", currencyName));
                                }))));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static void deleteCurrency(final Currency currency) {
        if (currencies().getCurrentCurrency().equals(currency)) {
            currencies().setCurrentCurrency(currencies().getDefaultCurrency());
        }
        currencies().deleteCurrency(currency);
    }

    // Virtual accounts

    private static Command.Parameterized virtualBalance() {
        final Parameter.Value<String> account = Parameter.remainingJoinedStrings().key("account").build();
        return Command.builder()
                .permission("economylite.admin.virtual.balance")
                .addParameter(account)
                .executor(context -> {
                    if (!context.hasPermission("economylite.admin.virtualbalance")) {
                        context.sendMessage(messages().get("command.noperm"));
                        return CommandResult.success();
                    }
                    final Optional<Account> found = EconomyLite.getEconomyService().findOrCreateAccount(context.requireOne(account));
                    if (found.isEmpty()) {
                        return error(context);
                    }
                    final LiteCurrency currency = current();
                    final BigDecimal bal = found.get().balance(currency);
                    context.sendMessage(messages().get("command.balanceother", "player", plain(found.get().displayName()), "balance",
                            LiteCurrency.formatAmount(bal), "label", currency.label(bal)));
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized virtualEcon() {
        final Command.Builder builder = Command.builder()
                .permission("economylite.admin.virtual")
                .executor(context -> {
                    context.sendMessage(messages().get("command.usage", "command", "/vecon", "subcommands", "add | set | remove <account> <amount>"));
                    return CommandResult.success();
                });
        for (final Change change : Change.values()) {
            builder.addChild(virtualChange(change), change.name);
        }
        return builder.build();
    }

    private static Command.Parameterized virtualChange(final Change change) {
        final Parameter.Value<String> account = account("account");
        final Parameter.Value<BigDecimal> amount = amount("amount");
        return Command.builder()
                .permission("economylite.admin.virtual." + change.name)
                .addParameters(account, amount)
                .executor(context -> {
                    final String target = context.requireOne(account);
                    final Optional<Account> found = EconomyLite.getEconomyService().findOrCreateAccount(target);
                    if (found.isEmpty()) {
                        return error(context);
                    }
                    final String name = plain(found.get().displayName());
                    if (change.apply(found.get(), current(), money(context.requireOne(amount)), CauseFactory.command(context.cause().cause()))) {
                        context.sendMessage(messages().get(change.success, "name", name));
                    } else {
                        context.sendMessage(messages().get(change.fail, "name", name));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    // /vpay <account> <player> <amount>: pays a player from a virtual account

    private static Command.Parameterized virtualPay() {
        final Parameter.Value<String> account = account("account");
        final Parameter.Value<UUID> player = player("target");
        final Parameter.Value<BigDecimal> amount = amount("amount");
        return Command.builder()
                .permission("economylite.admin.virtual.pay")
                .addParameters(account, player, amount)
                .executor(context -> {
                    final BigDecimal value = money(context.requireOne(amount));
                    if (value.signum() <= 0) {
                        context.sendMessage(messages().get("command.pay.invalid"));
                        return CommandResult.success();
                    }
                    final UUID target = context.requireOne(player);
                    final String targetName = LiteUniqueAccount.nameOf(target);
                    final Optional<Account> payer = EconomyLite.getEconomyService().findOrCreateAccount(context.requireOne(account));
                    final Optional<UniqueAccount> receiver = EconomyLite.getEconomyService().findOrCreateAccount(target);
                    if (payer.isEmpty() || receiver.isEmpty()) {
                        return error(context);
                    }
                    final LiteCurrency currency = current();
                    if (payer.get().transfer(receiver.get(), currency, value, CauseFactory.command(context.cause().cause())).result()
                            == ResultType.SUCCESS) {
                        context.sendMessage(messages().get("command.pay.success", "target", targetName, "amountandlabel",
                                amountAndLabel(value, currency)));
                        Sponge.server().player(target).ifPresent(p -> p.sendMessage(messages().get("command.pay.target", "amountandlabel",
                                amountAndLabel(value, currency), "sender", plain(payer.get().displayName()))));
                    } else {
                        context.sendMessage(messages().get("command.pay.failed", "target", targetName));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    // /payvirtual <account> <amount>: a player pays a virtual account

    private static Command.Parameterized payVirtual() {
        final Parameter.Value<String> account = account("target");
        final Parameter.Value<BigDecimal> amount = amount("amount");
        return Command.builder()
                .permission("economylite.virtual.pay")
                .addParameters(account, amount)
                .executor(context -> {
                    final Optional<ServerPlayer> source = playerSource(context);
                    if (source.isEmpty()) {
                        return CommandResult.success();
                    }
                    final BigDecimal value = money(context.requireOne(amount));
                    if (value.signum() <= 0) {
                        context.sendMessage(messages().get("command.pay.invalid"));
                        return CommandResult.success();
                    }
                    final Optional<Account> receiver = EconomyLite.getEconomyService().findOrCreateAccount(context.requireOne(account));
                    final Optional<UniqueAccount> payer = EconomyLite.getEconomyService().findOrCreateAccount(source.get().uniqueId());
                    if (receiver.isEmpty() || payer.isEmpty()) {
                        return error(context);
                    }
                    final LiteCurrency currency = current();
                    final String name = plain(receiver.get().displayName());
                    if (payer.get().transfer(receiver.get(), currency, value, CauseFactory.command(context.cause().cause())).result()
                            == ResultType.SUCCESS) {
                        context.sendMessage(messages().get("command.pay.success", "target", name, "amountandlabel", amountAndLabel(value, currency)));
                    } else {
                        context.sendMessage(messages().get("command.pay.failed", "target", name));
                    }
                    return CommandResult.success();
                })
                .build();
    }
}
