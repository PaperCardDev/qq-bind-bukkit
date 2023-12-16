package cn.paper_card.qq_bind;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.qq_bind.api.BindInfo;
import cn.paper_card.qq_bind.api.BindService;
import cn.paper_card.qq_bind.api.exception.AlreadyBindException;
import cn.paper_card.qq_bind.api.exception.QqHasBeenBindException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class BindServiceImpl implements BindService {

    record Cached(
            long time,
            BindInfo info
    ) {
    }

    private MySqlQqBindTable table = null;
    private Connection connection = null;
    private final DatabaseApi.MySqlConnection mySqlConnection;

    private final @NotNull ConcurrentHashMap<UUID, Cached> cacheByUuid;
    private final @NotNull ConcurrentHashMap<Long, Cached> cacheByQq;

    BindServiceImpl(DatabaseApi.MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
        this.cacheByUuid = new ConcurrentHashMap<>();
        this.cacheByQq = new ConcurrentHashMap<>();
    }

    private @NotNull MySqlQqBindTable getTable() throws SQLException {
        final Connection rowConnection = this.mySqlConnection.getRawConnection();

        if (this.connection != null && this.connection == rowConnection) {
            return this.table;
        }

        this.connection = rowConnection;
        if (this.table != null) this.table.close();
        this.table = new MySqlQqBindTable(rowConnection);

        // 清除缓存
        this.cacheByQq.clear();
        this.cacheByUuid.clear();

        return this.table;
    }

    void close() throws SQLException {
        // 清除缓存
        this.cacheByQq.clear();
        this.cacheByUuid.clear();

        synchronized (this.mySqlConnection) {
            final MySqlQqBindTable t = this.table;
            this.table = null;
            this.connection = null;

            if (t != null) t.close();
        }
    }

    @Override
    public void addBind(@NotNull BindInfo info) throws SQLException, AlreadyBindException, QqHasBeenBindException {
        synchronized (this.mySqlConnection) {
            try {
                final MySqlQqBindTable t = this.getTable();

                final BindInfo i1 = this.queryByUuid(info.uuid());
                this.mySqlConnection.setLastUseTime();

                if (i1 != null) {
                    throw new AlreadyBindException(i1, "%s (%s) 已经绑定了QQ：%d".formatted(
                            i1.name(), i1.uuid().toString(), i1.qq()
                    ));
                }

                final BindInfo i2 = this.queryByQq(info.qq());
                this.mySqlConnection.setLastUseTime();

                if (i2 != null) {
                    throw new QqHasBeenBindException(i2, "QQ[%d] 已经被 %s (%s) 绑定".formatted(
                            i2.qq(), i2.name(), i2.uuid().toString()
                    ));
                }

                final int inserted = t.insert(info);
                this.mySqlConnection.setLastUseTime();

                // 处理缓存
                this.cacheByQq.remove(info.qq());
                this.cacheByUuid.remove(info.uuid());

                if (inserted != 1) throw new RuntimeException("插入了%d条数据！".formatted(inserted));

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean removeBind(@NotNull UUID uuid, long qq) throws SQLException {
        synchronized (this.mySqlConnection) {
            final MySqlQqBindTable t;
            try {
                t = this.getTable();

                final int deleted = t.deleteByUuidAndQq(uuid, qq);
                this.mySqlConnection.setLastUseTime();

                // 处理缓存
                this.cacheByUuid.remove(uuid);
                this.cacheByQq.remove(qq);

                if (deleted == 1) return true;
                if (deleted == 0) return false;

                throw new RuntimeException("删除了%d条数据！".formatted(deleted));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }


    @Override
    public @Nullable BindInfo queryByUuid(@NotNull UUID uuid) throws SQLException {

        // 从缓存查询
        final Cached cached = this.cacheByUuid.get(uuid);
        if (cached != null) {
            return cached.info();
        }

        synchronized (this.mySqlConnection) {
            try {
                final MySqlQqBindTable t = this.getTable();

                final BindInfo info = t.queryByUuid(uuid);
                this.mySqlConnection.setLastUseTime();

                // 放入缓存
                this.cacheByUuid.put(uuid, new Cached(System.currentTimeMillis(), info));

                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable BindInfo queryByQq(long qq) throws SQLException {

        // 从缓存查询
        final Cached cached = this.cacheByQq.get(qq);
        if (cached != null) {
            return cached.info();
        }

        synchronized (this.mySqlConnection) {
            try {
                final MySqlQqBindTable t = this.getTable();

                final BindInfo info = t.queryByQq(qq);
                this.mySqlConnection.setLastUseTime();

                // 放入缓存
                this.cacheByQq.put(qq, new Cached(System.currentTimeMillis(), info));

                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean updateName(@NotNull UUID uuid, long qq, @NotNull String newName) throws Exception {
        synchronized (this.mySqlConnection) {
            try {
                final MySqlQqBindTable t = this.getTable();

                final int updated = t.updateName(uuid, qq, newName);
                this.mySqlConnection.setLastUseTime();

                if (updated == 0) return false;

                if (updated == 1) {
                    // 处理缓存
                    this.cacheByUuid.remove(uuid);
                    this.cacheByQq.remove(qq);

                    return true;
                }

                // 放入缓存
                throw new RuntimeException("更新了%d条数据！".formatted(updated));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    void clearCache() {
        this.cacheByQq.clear();
        this.cacheByUuid.clear();
    }
}
