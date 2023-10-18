package cn.paper_card.player_qq_bind;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
        this.addSubCommand(new ByQq());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
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
                plugin.sendError(commandSender, "你必须指定参数：玩家名或UUID！");
                return true;
            }

            if (argQq == null) {
                plugin.sendError(commandSender, "你必须指定参数：QQ号码！");
                return true;
            }

            final UUID uuid = parsePlayerArg(argPlayer);

            if (uuid == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            final long qq;

            try {
                qq = Long.parseLong(argQq);
            } catch (NumberFormatException ignored) {
                plugin.sendError(commandSender, "不正确的QQ号码：%s".formatted(argQq));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                final QqBindApi.BindInfo bindInfo;

                if (qq > 0) {
                    try {
                        bindInfo = plugin.queryByQq(qq);
                    } catch (Exception e) {
                        e.printStackTrace();
                        plugin.sendError(commandSender, e.toString());
                        return;
                    }

                    if (bindInfo != null) {
                        final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(bindInfo.uuid());
                        String name = offlinePlayer.getName();
                        if (name == null) name = offlinePlayer.getUniqueId().toString();
                        plugin.sendWarning(commandSender, "该QQ[%d]已经绑定了%s".formatted(qq, name));
                        return;
                    }
                }

                final boolean added;

                try {
                    added = plugin.addOrUpdateByUuid(uuid, qq);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
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

                plugin.sendInfo(commandSender, text.build());
            });

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
                plugin.sendError(commandSender, "你必须指定参数：玩家名或UUID！");
                return true;
            }

            final UUID uuid = parsePlayerArg(argPlayer);

            if (uuid == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final String name = getPlayerName(uuid);

                final QqBindApi.BindInfo bindInfo;

                try {
                    bindInfo = plugin.queryByUuid(uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (bindInfo == null) {
                    plugin.sendWarning(commandSender, "该玩家[%s]没有绑定QQ！".formatted(name));
                    return;
                }

                final String uuidStr = bindInfo.uuid().toString();
                final String qqStr = "%d".formatted(bindInfo.qq());

                plugin.sendInfo(commandSender, plugin.buildInfoComponent(name, uuidStr, qqStr));
            });

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

    private class ByQq extends TheMcCommand {

        private final @NotNull Permission permission;

        protected ByQq() {
            super("by-qq");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            final String argQq = strings.length > 0 ? strings[0] : null;
            if (argQq == null) {
                plugin.sendError(commandSender, "你必须提供参数：QQ号码");
                return true;
            }

            final long qq;

            try {
                qq = Long.parseLong(argQq);
            } catch (NumberFormatException e) {
                plugin.sendError(commandSender, "%s 不是正确的QQ号码".formatted(argQq));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApi.BindInfo bindInfo;

                try {
                    bindInfo = plugin.queryByQq(qq);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (bindInfo == null) {
                    plugin.sendWarning(commandSender, "QQ[%d]没有绑定UUID".formatted(qq));
                    return;
                }

                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(bindInfo.uuid());
                String name = offlinePlayer.getName();
                if (name == null) name = offlinePlayer.getUniqueId().toString();

                plugin.sendInfo(commandSender, plugin.buildInfoComponent(name, offlinePlayer.getUniqueId().toString(), argQq));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String argQq = strings[0];
                if (argQq.isEmpty()) {
                    final LinkedList<String> list = new LinkedList<>();
                    list.add("<QQ>");
                    return list;
                }
            }
            return null;
        }
    }
}
