package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class BindCodeApiImpl implements QqBindApi.BindCodeApi {

    BindCodeApiImpl(@NotNull DatabaseApi.MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;

    private BindCodeTable table = null;
    private Connection connection = null;

    private final AtomicInteger count = new AtomicInteger(-1);

    // 验证有效时间，以ms为单位
    private static final long MAX_ALIVE_TIME = 60 * 1000L;


    private @NotNull BindCodeTable getTable() throws SQLException {
        final Connection rowConnection = this.mySqlConnection.getRowConnection();

        if (this.connection != null && this.connection == rowConnection) {
            return this.table;
        }

        this.connection = rowConnection;
        if (this.table != null) this.table.close();
        this.table = new BindCodeTable(rowConnection);

        this.count.set(this.table.queryCount());

        return this.table;
    }

    private int randomCode() {
        final int min = 1;
        final int max = 999999;
        return new Random().nextInt(max - min + 1) + min;
    }


    @Override
    public int createCode(@NotNull UUID uuid, @NotNull String name) throws SQLException, DuplicatedCode {

        synchronized (this.mySqlConnection) {
            final int code = this.randomCode();
            final long time = System.currentTimeMillis();

            try {
                final BindCodeTable t = this.getTable();

                // 为了防止验证码重复
                {
                    final QqBindApi.BindCodeInfo info = t.queryByCode(code);
                    this.mySqlConnection.setLastUseTime();

                    if (info != null) throw new DuplicatedCode();
                }

                final QqBindApi.BindCodeInfo info = new QqBindApi.BindCodeInfo(code, name, uuid, time);

                final int updated = t.updateByUuid(info);
                this.mySqlConnection.setLastUseTime();

                if (updated == 0) {
                    final int inserted = t.insert(info);

                    this.count.set(this.table.queryCount());

                    if (inserted != 1) throw new RuntimeException("插入了%d条数据！".formatted(inserted));

                    return code;
                }

                if (updated == 1) return code;

                throw new RuntimeException("根据一个UUID更新了%d条数据！".formatted(updated));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    // 取出一个绑定验证码
    @Override
    public @Nullable QqBindApi.BindCodeInfo takeByCode(int code) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final BindCodeTable t = this.getTable();

                final QqBindApi.BindCodeInfo info = t.queryByCode(code);
                this.mySqlConnection.setLastUseTime();

                if (info == null) return null;

                final int deleted = t.deleteByCode(code);
                this.mySqlConnection.setLastUseTime();

                this.count.set(this.table.queryCount());

                if (deleted != 1) throw new RuntimeException("删除了%d条数据！".formatted(deleted));

                // 判断验证码是否过期
                final long delta = System.currentTimeMillis() - info.time();
                if (delta > MAX_ALIVE_TIME) return null;

                return info;

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @NotNull List<String> queryPlayerNames() throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final List<String> names = this.getTable().queryNames();
                this.mySqlConnection.setLastUseTime();

                this.count.set(names.size());

                return names;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public int cleanOutdated() throws SQLException {
        final long begin = System.currentTimeMillis() - MAX_ALIVE_TIME;
        synchronized (this.mySqlConnection) {

            final int deleted;
            try {
                deleted = this.getTable().deleteTimeBefore(begin);
                this.mySqlConnection.setLastUseTime();

                this.count.set(this.table.queryCount());

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
            return deleted;
        }
    }

    @Override
    public long getMaxAliveTime() {
        return MAX_ALIVE_TIME;
    }

    @Override
    public int getCodeCount() {
        return this.count.get();
    }

    int close() throws SQLException {
        synchronized (this.mySqlConnection) {

            final int c;
            try {
                c = this.cleanOutdated();
            } catch (SQLException e) {
                final BindCodeTable t = this.table;
                this.table = null;
                try {
                    t.close();
                } catch (SQLException ignored) {
                }

                throw e;
            }

            final BindCodeTable t = this.table;
            this.table = null;
            t.close();

            return c;
        }
    }
}