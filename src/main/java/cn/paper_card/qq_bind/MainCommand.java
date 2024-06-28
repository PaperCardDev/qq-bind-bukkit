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

class MainCommand extends NewMcCommand.HasSub {

    private final @NotNull ThePlugin plugin;
    private final @NotNull Permission permission;

    public MainCommand(@NotNull ThePlugin plugin) {
        super("qq-bind");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("qq-bind.command"));

        this.addSub(new Get());
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

        private final @NotNull Permission permSelf;
        private final @NotNull Permission permOther;

        protected Get() {
            super("get");
            this.permSelf = plugin.addPermission(MainCommand.this.permission + ".get.self");
            this.permOther = plugin.addPermission(MainCommand.this.permission + ".get.other");
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            plugin.appendPrefix(text);
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return true;
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

                text.appendNewline();
                text.append(Component.text("QQ: "));
                text.append(Component.text(info.qq()));

                text.appendNewline();
                text.append(Component.text("UUID: "));
                text.append(Component.text(info.uuid()));

                text.appendNewline();
                text.append(Component.text("时间: "));
                text.append(Component.text(info.time()));

                text.appendNewline();
                text.append(Component.text("备注: "));
                text.append(Component.text(info.remark()));

                sender.sendMessage(text.build().color(NamedTextColor.GREEN));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return null;
        }
    }
}