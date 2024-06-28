package cn.paper_card.qq_bind;

import cn.paper_card.mc_command.NewMcCommand;
import cn.paper_card.qq_bind.api.BindInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

class MainCommand extends NewMcCommand.HasSub {

    private final @NotNull ThePlugin plugin;
    private final @NotNull Permission permission;

    public MainCommand(@NotNull ThePlugin plugin) {
        super("qq-bind");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("qq-bind.command"));

        this.addSub(new Get());
        this.addSub(new Qq());
        this.addSub(new Add());
        this.addSub(new Remove());
    }

    @Override
    protected boolean canExecute(@NotNull CommandSender commandSender) {
        return commandSender.hasPermission(this.permission);
    }

    @Override
    protected void appendPrefix(TextComponent.@NotNull Builder text) {
        plugin.appendPrefix(text);
    }

    class Get extends NewMcCommand {

        private final @NotNull Permission permission;

        protected Get() {
            super("get");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            plugin.appendPrefix(text);
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

            final String argPlayer = args.length > 0 ? args[0] : null;

            final Sender sd = new Sender(sender);

            if (argPlayer == null) {
                sd.error("必须指定参数：玩家名或UUID");
                return true;
            }

            final OfflinePlayer offlinePlayer = NewMcCommand.parseOfflinePlayerName(argPlayer, plugin.getServer());

            if (offlinePlayer == null) {
                sd.error("找不到玩家：" + argPlayer);
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApiImpl api = plugin.getQqBindApi();

                final BindInfo info;

                try {
                    info = api.queryByUuid(offlinePlayer.getUniqueId());
                } catch (Exception e) {
                    plugin.getSLF4JLogger().error("", e);
                    sd.exception(e);
                    return;
                }

                if (info == null) {
                    sd.warning(argPlayer + " 没有绑定QQ！");
                    return;
                }

                final TextComponent.Builder text = Component.text();
                sd.appendPrefix(text);
                text.appendSpace();
                text.append(Component.text("==== QQ绑定信息 ===="));

                MyUtil.appendBindInfo(text, info, offlinePlayer.getName());

                sender.sendMessage(text.build().color(NamedTextColor.GREEN));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return null;
        }
    }

    class Qq extends NewMcCommand {

        private final @NotNull Permission permission;

        protected Qq() {
            super("qq");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            plugin.appendPrefix(text);
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

            final String argQq = args.length > 0 ? args[0] : null;

            final Sender sd = new Sender(sender);

            if (argQq == null) {
                sd.error("必须指定参数：QQ号");
                return true;
            }

            final long qq;

            try {
                qq = Long.parseLong(argQq);
            } catch (NumberFormatException e) {
                sd.error("%s 不是正确的QQ号！".formatted(argQq));
                return true;
            }


            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApiImpl api = plugin.getQqBindApi();

                final BindInfo info;

                try {
                    info = api.queryByQq(qq);
                } catch (Exception e) {
                    plugin.getSLF4JLogger().error("", e);
                    sd.exception(e);
                    return;
                }

                if (info == null) {
                    sd.warning("QQ[%d]没有被任何玩家绑定！".formatted(qq));
                    return;
                }

                final TextComponent.Builder text = Component.text();
                sd.appendPrefix(text);
                text.appendSpace();
                text.append(Component.text("==== QQ绑定信息 ===="));

                MyUtil.appendBindInfo(text, info, plugin.getServer().getOfflinePlayer(UUID.fromString(info.uuid())).getName());

                sender.sendMessage(text.build().color(NamedTextColor.GREEN));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return null;
        }
    }

    class Add extends NewMcCommand {

        private final @NotNull Permission permission;

        protected Add() {
            super("add");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            final String argPlayer = args.length > 0 ? args[0] : null;
            final String argQq = args.length > 1 ? args[1] : null;

            final Sender sd = new Sender(sender);

            if (argPlayer == null) {
                sd.error("必须提供参数：玩家名或UUID");
                return true;
            }
            if (argQq == null) {
                sd.error("必须提供参数：QQ号");
                return true;
            }

            final OfflinePlayer offlinePlayer = NewMcCommand.parseOfflinePlayerName(argPlayer, plugin.getServer());

            if (offlinePlayer == null) {
                sd.error("找不到玩家: " + argPlayer);
                return true;
            }


            final long qq;
            try {
                qq = Long.parseLong(argQq);
            } catch (NumberFormatException e) {
                sd.error("不正确的QQ号码: " + argQq);
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApiImpl api = plugin.getQqBindApi();

                try {
                    api.addBind(offlinePlayer.getUniqueId(), qq, "管理员%s使用指令添加".formatted(sender.getName()));
                } catch (Exception e) {
                    plugin.getSLF4JLogger().error("", e);
                    sd.exception(e);
                    return;
                }

                sd.info("添加QQ绑定成功 :D");
            });

            return true;
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            plugin.appendPrefix(text);
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return null;
        }
    }

    class Remove extends NewMcCommand {

        private final @NotNull Permission permission;

        protected Remove() {
            super("remove");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            final String argPlayer = args.length > 0 ? args[0] : null;


            final Sender sd = new Sender(sender);

            if (argPlayer == null) {
                sd.error("必须提供参数：玩家名或UUID");
                return true;
            }


            final OfflinePlayer offlinePlayer = NewMcCommand.parseOfflinePlayerName(argPlayer, plugin.getServer());

            if (offlinePlayer == null) {
                sd.error("找不到玩家: " + argPlayer);
                return true;
            }


            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final QqBindApiImpl api = plugin.getQqBindApi();

                try {
                    api.removeBind(offlinePlayer.getUniqueId(), 0);
                } catch (Exception e) {
                    plugin.getSLF4JLogger().error("", e);
                    sd.exception(e);
                    return;
                }

                sd.info("删除QQ绑定成功 :D");
            });

            return true;
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            plugin.appendPrefix(text);
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return null;
        }
    }
}
