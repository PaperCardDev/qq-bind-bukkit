package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import cn.paper_card.database.DatabaseConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public final class PlayerQqBind extends JavaPlugin implements QqBindApi {

    private final @NotNull Object lock = new Object();
    private DatabaseConnection connection = null;
    private QqBindTable table = null;

    private final @NotNull BindCodeApiImpl bindCodeService;

    public PlayerQqBind() {
        this.bindCodeService = new BindCodeApiImpl(this);
    }

    @NotNull DatabaseApi getDatabaseApi() throws Exception {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("Database");
        if (!(plugin instanceof DatabaseApi api)) throw new Exception("Database插件未安装！");
        return api;
    }

    private @NotNull DatabaseConnection getConnection() throws Exception {
        if (this.connection == null) {
            this.connection = this.getDatabaseApi().connectImportant();
        }

        return this.connection;
    }

    private @NotNull QqBindTable getTable() throws Exception {
        if (this.table == null) {
            this.table = new QqBindTable(this.getConnection().getConnection());
        }
        return this.table;
    }

    @Override
    public void onEnable() {
        synchronized (this.lock) {
            try {
                this.getTable();
            } catch (Exception e) {
                getLogger().severe(e.toString());
                e.printStackTrace();
            }
        }

        final PluginCommand command = this.getCommand("qq-bind");
        final TheCommand theCommand = new TheCommand(this);
        assert command != null;
        command.setExecutor(theCommand);
        command.setTabCompleter(theCommand);
    }

    @Override
    public void onDisable() {
        synchronized (this.lock) {
            if (this.table != null) {
                try {
                    this.table.close();
                } catch (SQLException e) {
                    getLogger().severe(e.toString());
                    e.printStackTrace();
                }
                this.table = null;
            }

            if (this.connection != null) {
                try {
                    this.connection.close();
                } catch (SQLException e) {
                    getLogger().severe(e.toString());
                    e.printStackTrace();
                }
                this.connection = null;
            }

        }

        this.bindCodeService.close();
    }

    @Override
    public @NotNull BindCodeApi getBindCodeApi() {
        return this.bindCodeService;
    }

    @Override
    public boolean addOrUpdateByUuid(@NotNull UUID uuid, long qq) throws Exception {
        synchronized (this.lock) {
            final QqBindTable t = this.getTable();
            final int updated = t.updateQqByUuid(uuid, qq);
            if (updated == 0) {
                final int insert = t.insert(new BindInfo(uuid, qq));
                if (insert != 1) throw new Exception("插入了%d条（应该是1条）数据！".formatted(insert));
                return true;
            }
            if (updated == 1) return false;
            throw new Exception("根据一个UUID更新了%d条数据！".formatted(updated));
        }
    }

    @Override
    public @Nullable BindInfo queryByUuid(@NotNull UUID uuid) throws Exception {
        synchronized (this.lock) {
            final QqBindTable t = this.getTable();
            final List<BindInfo> list = t.queryByUuid(uuid);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) return list.get(0);
            throw new Exception("根据一个UUID查询到多条数据！");
        }
    }

    @Override
    public @Nullable BindInfo queryByQq(long qq) throws Exception {
        synchronized (this.lock) {
            final QqBindTable t = this.getTable();
            final List<BindInfo> list = t.queryByQq(qq);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) return list.get(0);
            throw new Exception("根据一个QQ号查询到多条数据！");
        }
    }

    @Override
    public long onPreLoginCheck(@NotNull AsyncPlayerPreLoginEvent event, @Nullable QqBot qqBot, @Nullable AutoQqBind autoQqBind) {
        final UUID id = event.getUniqueId();

        final BindInfo bindInfo;

        // 查询绑定信息
        try {
            bindInfo = this.queryByUuid(id);
        } catch (Exception e) {
            e.printStackTrace();
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.kickMessage(Component.text(e.toString()).color(NamedTextColor.RED));
            return 0;
        }

        // 已经绑定了QQ
        if (bindInfo != null && bindInfo.qq() > 0) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            return bindInfo.qq();
        }

        // 没有绑定QQ

        // 尝试自动绑定
        if (autoQqBind != null) {
            final long qq = autoQqBind.tryBind(id, event.getName());
            if (qq > 0) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
                return qq;
            }
        }

        // 无法自动绑定
        final int code;

        try {
            code = this.bindCodeService.createCode(id, event.getName());
        } catch (Exception e) {
            e.printStackTrace();
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.kickMessage(Component.text(e.toString()).color(NamedTextColor.RED));
            return 0;
        }

        if (code == 0) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.kickMessage(Component.text("此次生成的随机验证码重复，请尝试重新连接以重新生成").color(NamedTextColor.RED));
            return 0;
        }

        final TextComponent.Builder text = Component.text();
        text.append(Component.text("[你没有绑定QQ！]").color(NamedTextColor.RED));
        text.append(Component.newline());

        text.append(Component.text("QQ绑定验证码："));
        text.append(Component.text(code).color(NamedTextColor.AQUA));
        text.append(Component.newline());

        text.append(Component.text("请在我们的QQ群里直接发送该数字验证码"));
        text.append(Component.newline());
        text.append(Component.text("如果QQ机器人在线（当前状态：%s），会自动处理您发送的验证码".formatted(qqBot != null ? "在线" : "不在线")));
        text.append(Component.newline());

        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
        event.kickMessage(text.build());

        return 0;
    }

    @Override
    public @Nullable List<String> onMainGroupMessage(long qq, @NotNull String message) {

        // 没有任何一个账号等待绑定
        if (this.bindCodeService.getPlayerCount() <= 0) return null;

        // 解析验证码
        final int code;
        try {
            code = Integer.parseInt(message);
        } catch (NumberFormatException ignored) {
            return null;
        }

        final LinkedList<String> reply = new LinkedList<>();

        // 检查QQ绑定
        final BindInfo bindInfo;

        try {
            bindInfo = this.queryByQq(qq);
        } catch (Exception e) {
            e.printStackTrace();
            reply.add(e.toString());
            return reply;
        }

        // 已经绑定，不处理
        if (bindInfo != null && bindInfo.qq() > 0) return null;

        // 取出验证码信息
        final BindCodeInfo bindCodeInfo;
        try {
            bindCodeInfo = this.bindCodeService.takeByCode(code);
        } catch (Exception e) {
            e.printStackTrace();
            reply.add(e.toString());
            return reply;
        }

        if (bindCodeInfo == null) {
            reply.add("不存在或过期的验证码：%d，请尝试重新连接获取新的验证码！".formatted(code));
            return reply;
        }

        // 添加绑定
        final boolean added;

        try {
            added = this.addOrUpdateByUuid(bindCodeInfo.uuid(), qq);
        } catch (Exception e) {
            e.printStackTrace();
            reply.add(e.toString());
            return reply;
        }

        this.getLogger().info("%sQQ绑定 {玩家: %s, QQ: %d}".formatted((added ? "添加" : "更新"), bindCodeInfo.name(), qq));

        reply.add("%sQQ绑定成功\n游戏名：%s\n快连接服务器试试叭~".formatted((added ? "添加" : "更新"), bindCodeInfo.name()));

        // 清理过期验证码
        final int deleted;
        try {
            deleted = this.bindCodeService.cleanOutdated();
        } catch (Exception e) {
            e.printStackTrace();
            reply.add(e.toString());
            return reply;
        }

        if (deleted > 0) {
            this.getLogger().info("清理了%d个过期的验证码".formatted(deleted));
        }

        return reply;
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }
}
