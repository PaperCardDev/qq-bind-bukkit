package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import cn.paper_card.database.DatabaseConnection;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
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

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }
}
