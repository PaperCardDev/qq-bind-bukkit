package cn.paper_card.player_qq_bind;

import cn.paper_card.MojangProfileApi;
import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

class TheCommand extends TheMcCommand.HasSub {

    private final @NotNull ThePlugin plugin;

    private final @NotNull Permission permission;

    private final @NotNull MojangProfileApi mojangProfileApi;


    public TheCommand(@NotNull ThePlugin plugin) {
        super("qq-bind");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(this.plugin.getServer().getPluginManager().getPermission("qq-bind.command"));

        final PluginCommand command = plugin.getCommand(this.getLabel());
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);

        this.addSubCommand(new Add());
        this.addSubCommand(new Remove());
        this.addSubCommand(new Get());
        this.addSubCommand(new Qq());
        this.addSubCommand(new Code());
        this.addSubCommand(new Reload());

        this.mojangProfileApi = new MojangProfileApi();
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    private @Nullable MojangProfileApi.Profile parsePlayerArg(@NotNull String arg) {

        try {
            final UUID uuid = UUID.fromString(arg);
            return new MojangProfileApi.Profile(null, uuid);
        } catch (IllegalArgumentException ignored) {
        }

        for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {

            final String name = offlinePlayer.getName();
            if (name == null) continue;

            if (name.equals(arg)) return new MojangProfileApi.Profile(name, offlinePlayer.getUniqueId());
        }

        return null;
    }

    private @NotNull List<String> tabCompletePlayerName(@NotNull String arg, @NotNull String tip) {
        final LinkedList<String> list = new LinkedList<>();

        if (arg.isEmpty()) {
            list.add(tip);
        } else {
            for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
                final String name = offlinePlayer.getName();
                if (name == null) continue;
                if (name.startsWith(arg)) list.add(name);
            }
        }

        return list;
    }

    private class Add extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Add() {
            super("add");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
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


            final MojangProfileApi.Profile profile = parsePlayerArg(argPlayer);

            if (profile == null) {
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
                final MojangProfileApi.Profile p;

                if (profile.name() == null) {
                    try {
                        p = mojangProfileApi.requestByUuid(profile.uuid());
                    } catch (Exception e) {
                        plugin.getQqBindApi().handleException(e);
                        plugin.sendException(commandSender, e);
                        return;
                    }
                } else {
                    p = profile;
                }

                String remark = plugin.getConfigManager().getRemarkForByAdd();
                remark = remark.replace("%operator%", commandSender.getName());

                // 添加绑定
                final QqBindApi.BindInfo info = new QqBindApi.BindInfo(
                        p.uuid(), p.name(), qq,
                        remark.formatted(commandSender.getName()),
                        System.currentTimeMillis()
                );

                try {
                    plugin.getQqBindApi().getBindApi().addBind(info);
                }

                // 已经绑定了
                catch (QqBindApi.BindApi.AreadyBindException e) {

                    final QqBindApi.BindInfo bindInfo = e.getBindInfo();
                    plugin.sendWarning(commandSender, "玩家 %s 已经绑定了QQ：%d".formatted(
                            p.name(), bindInfo.qq()
                    ));

                    return;
                }

                // QQ号被绑定了
                catch (QqBindApi.BindApi.QqHasBeenBindedException e) {

                    final QqBindApi.BindInfo bindInfo = e.getBindInfo();

                    plugin.sendWarning(commandSender, "QQ[%d] 已经被玩家 %s 绑定".formatted(
                            bindInfo.qq(), bindInfo.name()
                    ));
                    return;
                }

                // 其他错误
                catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                final TextComponent.Builder text = Component.text();
                text.append(Component.text("添加QQ绑定成功").color(NamedTextColor.GREEN));

                text.appendNewline();
                plugin.getQqBindApi().appendInfo(text, info);

                plugin.sendInfo(commandSender, text.build());
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String argPlayer = strings[0];
                return tabCompletePlayerName(argPlayer, "<玩家名或UUID'>");
            }

            if (strings.length == 2) {
                final LinkedList<String> list = new LinkedList<>();
                final String argQq = strings[1];
                if (argQq.isEmpty()) list.add("<QQ号>");
                return list;
            }
            return null;
        }
    }

    private class Remove extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Remove() {
            super("remove");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + '.' + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            final String argPlayer = strings.length > 0 ? strings[0] : null;
            if (argPlayer == null) {
                plugin.sendError(commandSender, "必须提供参数：玩家名或UUID");
                return true;
            }

            final MojangProfileApi.Profile profile = parsePlayerArg(argPlayer);
            if (profile == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApi.BindInfo info;
                try {
                    info = plugin.getQqBindApi().getBindApi().queryByUuid(profile.uuid());
                } catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (info == null) {
                    plugin.sendWarning(commandSender, "该玩家没有绑定QQ");
                    return;
                }

                final boolean deleted;
                try {
                    deleted = plugin.getQqBindApi().getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
                } catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (!deleted) {
                    plugin.sendError(commandSender, "未知错误");
                    return;
                }

                final TextComponent.Builder text = Component.text();
                text.append(Component.text("删除QQ绑定成功").color(NamedTextColor.GREEN));

                text.appendNewline();
                plugin.getQqBindApi().appendInfo(text, info);

                plugin.sendInfo(commandSender, text.build());
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String string = strings[0];
                return tabCompletePlayerName(string, "<玩家名或UUID>");
            }
            return null;
        }
    }

    private class Get extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Get() {
            super("get");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            // <玩家名或UUID>
            final String argPlayer = strings.length > 0 ? strings[0] : null;

            final MojangProfileApi.Profile profile;

            if (argPlayer == null) {
                if (commandSender instanceof final Player player) {
                    profile = new MojangProfileApi.Profile(player.getName(), player.getUniqueId());
                } else {
                    plugin.sendError(commandSender, "你必须指定参数：玩家名或UUID！");
                    return true;
                }
            } else {
                profile = parsePlayerArg(argPlayer);

                if (profile == null) {
                    plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                    return true;
                }
            }


            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApi.BindInfo info;

                try {
                    info = plugin.getQqBindApi().getBindApi().queryByUuid(profile.uuid());
                } catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                String name = profile.name();
                if (name == null) {
                    try {
                        name = mojangProfileApi.requestByUuid(profile.uuid()).name();
                    } catch (Exception e) {
                        plugin.getQqBindApi().handleException(e);
                        name = profile.uuid().toString();
                    }
                }

                if (info == null) {
                    plugin.sendWarning(commandSender, "%s 没有绑定QQ".formatted(name));
                    return;
                }

                final TextComponent.Builder text = Component.text();

                plugin.getQqBindApi().appendInfo(text, info);

                plugin.sendInfo(commandSender, text.build());
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String arg = strings[0];
                return tabCompletePlayerName(arg, "<玩家名或UUID>");
            }
            return null;
        }
    }

    private class Qq extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Qq() {
            super("qq");
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
                    bindInfo = plugin.getQqBindApi().getBindApi().queryByQq(qq);
                } catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (bindInfo == null) {
                    plugin.sendWarning(commandSender, "该QQ[%d] 没有被任何玩家绑定".formatted(qq));
                    return;
                }

                final TextComponent.Builder text = Component.text();
                plugin.getQqBindApi().appendInfo(text, bindInfo);

                plugin.sendInfo(commandSender, text.build());
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

    private class Code extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Code() {
            super("code");
            this.permission = new Permission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (!(commandSender instanceof final Player player)) {
                plugin.sendError(commandSender, "该命令只能由玩家执行");
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApi.BindInfo bindInfo;

                try {
                    bindInfo = plugin.getQqBindApi().getBindApi().queryByUuid(player.getUniqueId());
                } catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (bindInfo != null) {
                    plugin.sendWarning(commandSender, "你已经绑定到了QQ[%d]，无需生成绑定验证码，如需改绑，请联系管理员".formatted(bindInfo.qq()));
                    return;
                }

                final int code;

                try {
                    code = plugin.getQqBindApi().getBindCodeApi().createCode(player.getUniqueId(), player.getName());
                }
                //
                catch (QqBindApi.BindCodeApi.DuplicatedCode e) {
                    plugin.sendWarning(commandSender, "此处生成的验证码重复（小概率事件），请重试");
                    return;
                }
                //
                catch (Exception e) {
                    plugin.getQqBindApi().handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                final TextComponent.Builder text = Component.text();


                text.append(Component.text("QQ绑定验证码：").color(NamedTextColor.GREEN));

                final String codeStr = "%d".formatted(code);
                text.append(Component.text(codeStr).color(NamedTextColor.GOLD).decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.copyToClipboard(codeStr))
                        .hoverEvent(HoverEvent.showText(Component.text("点击复制"))));

                text.append(Component.text("，有效时间：").color(NamedTextColor.GREEN));
                text.append(Component.text(plugin.getQqBindApi().toReadableTime(plugin.getQqBindApi()
                        .getBindCodeApi().getMaxAliveTime())).color(NamedTextColor.YELLOW));

                text.appendNewline();
                text.append(Component.text("请直接将这个数字发送到QQ主群中，如果QQ机器人在线，会自动处理").color(NamedTextColor.GREEN));

                plugin.sendInfo(commandSender, text.build());
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }

    private class Reload extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Reload() {
            super("reload");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            plugin.reloadConfig();
            plugin.getQqBindApi().getBindApi().clearCache();
            plugin.sendInfo(commandSender, Component.text("已重载配置").color(NamedTextColor.GREEN));
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }
}
