package cn.paper_card.player_qq_bind;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class TheCommand extends TheMcCommand.HasSub {

    private final @NotNull PlayerQqBind plugin;

    private final @NotNull Permission permission;


    public TheCommand(@NotNull PlayerQqBind plugin) {
        super("qq-bind");
        this.plugin = plugin;
        this.permission = this.plugin.addPermission("qq-bind.command");

        this.addSubCommand(new Set());
        this.addSubCommand(new Get());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    private static void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text(error).color(NamedTextColor.DARK_RED));
    }

    private @Nullable UUID parsePlayerArg(@NotNull String arg) {
        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException ignored) {
        }

        for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {

            final String name = offlinePlayer.getName();
            if (name == null) continue;

            if (name.equals(arg)) return offlinePlayer.getUniqueId();
        }

        return null;
    }

    private @NotNull String getPlayerName(@NotNull UUID uuid) {
        for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
            if (offlinePlayer.getUniqueId().equals(uuid)) {
                final String name = offlinePlayer.getName();
                if (name != null) return name;
            }
        }
        return uuid.toString();
    }

    private class Set extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Set() {
            super("set");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + ".set");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            // <玩家名或UUID> <QQ号码>

            final String argPlayer = strings.length > 0 ? strings[0] : null;
            final String argQq = strings.length > 1 ? strings[1] : null;

            if (argPlayer == null) {
                sendError(commandSender, "你必须指定参数：玩家名或UUID！");
                return true;
            }

            if (argQq == null) {
                sendError(commandSender, "你必须指定参数：QQ号码！");
                return true;
            }

            final UUID uuid = parsePlayerArg(argPlayer);

            if (uuid == null) {
                sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            final long qq;

            try {
                qq = Long.parseLong(argQq);
            } catch (NumberFormatException ignored) {
                sendError(commandSender, "不正确的QQ号码：%s".formatted(argQq));
                return true;
            }


            final boolean added;

            try {
                added = plugin.addOrUpdateByUuid(uuid, qq);
            } catch (Exception e) {
                sendError(commandSender, e.toString());
                return true;
            }

            final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);

            final TextComponent.Builder text = Component.text();
            text.append(Component.text("%s了QQ绑定：".formatted(added ? "添加" : "更新")));
            text.append(Component.newline());

            text.append(Component.text("玩家名: %s".formatted(offlinePlayer.getName())));
            text.append(Component.newline());

            text.append(Component.text("UUID: %s".formatted(offlinePlayer.getUniqueId())));
            text.append(Component.newline());

            text.append(Component.text("QQ: %d".formatted(qq)));

            commandSender.sendMessage(text.build());

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String argPlayer = strings[0];
                final LinkedList<String> list = new LinkedList<>();
                if (argPlayer.isEmpty()) list.add("<玩家名或UUID>");
                for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
                    final String name = offlinePlayer.getName();
                    if (name == null) continue;
                    if (name.startsWith(argPlayer)) list.add(name);
                }
                return list;
            }

            if (strings.length == 2) {
                final LinkedList<String> list = new LinkedList<>();
                final String argQq = strings[1];
                if (argQq.isEmpty()) list.add("<QQ号>");
                if ("0".startsWith(argQq)) list.add("0");
                return list;
            }

            return null;
        }
    }

    private class Get extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Get() {
            super("get");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + ".get");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            // <玩家名或UUID>
            final String argPlayer = strings.length > 0 ? strings[0] : null;

            if (argPlayer == null) {
                sendError(commandSender, "你必须指定参数：玩家名或UUID！");
                return true;
            }

            final UUID uuid = parsePlayerArg(argPlayer);

            if (uuid == null) {
                sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            final String name = getPlayerName(uuid);

            final QqBindApi.BindInfo bindInfo;

            try {
                bindInfo = plugin.queryByUuid(uuid);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(commandSender, e.toString());
                return true;
            }

            if (bindInfo == null) {
                commandSender.sendMessage(Component.text("该玩家[%s]没有绑定QQ！".formatted(name)));
                return true;
            }

            final String uuidStr = bindInfo.uuid().toString();
            final String qqStr = "%d".formatted(bindInfo.qq());

            commandSender.sendMessage(Component.text()

                    .append(Component.text("QQ绑定信息："))
                    .append(Component.newline())

                    .append(Component.text("玩家名: "))
                    .append(Component.text(name).color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.copyToClipboard(name)))
                    .append(Component.newline())

                    .append(Component.text("UUID: "))
                    .append(Component.text(uuidStr).color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.copyToClipboard(uuidStr)))
                    .append(Component.newline())

                    .append(Component.text("QQ: "))
                    .append(Component.text(qqStr).color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.copyToClipboard(qqStr)))

                    .build());

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String arg = strings[0];

                final LinkedList<String> list = new LinkedList<>();

                if (arg.isEmpty()) list.add("<玩家名或UUID>");

                for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {

                    final String name = offlinePlayer.getName();
                    if (name == null) continue;

                    if (name.startsWith(arg)) list.add(name);
                }
                return list;
            }
            return null;
        }
    }
}
