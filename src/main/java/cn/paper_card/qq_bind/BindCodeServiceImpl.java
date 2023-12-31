package cn.paper_card.qq_bind;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.qq_bind.api.BindCodeInfo;
import cn.paper_card.qq_bind.api.BindCodeService;
import cn.paper_card.qq_bind.api.exception.DuplicatedCodeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class BindCodeServiceImpl implements BindCodeService {

    BindCodeServiceImpl(@NotNull DatabaseApi.MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;

    private BindCodeTable table = null;
    private Connection connection = null;

    private final AtomicInteger count = new AtomicInteger(-1);

    // 验证有效时间，以ms为单位
    private static final long MAX_ALIVE_TIME = 60 * 1000L;


    private @NotNull BindCodeTable getTable() throws SQLException {
        final Connection rowConnection = this.mySqlConnection.getRawConnection();

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
    public int createCode(@NotNull UUID uuid, @NotNull String name) throws SQLException, DuplicatedCodeException {

        synchronized (this.mySqlConnection) {
            final int code = this.randomCode();
            final long time = System.currentTimeMillis();

            try {
                final BindCodeTable t = this.getTable();

                // 为了防止验证码重复
                {
                    final BindCodeInfo info = t.queryByCode(code);
                    this.mySqlConnection.setLastUseTime();

                    if (info != null) throw new DuplicatedCodeException();
                }

                final var info = new BindCodeInfo(code, name, uuid, time);

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
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    // 取出一个绑定验证码
    @Override
    public @Nullable BindCodeInfo takeByCode(int code) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final BindCodeTable t = this.getTable();

                final BindCodeInfo info = t.queryByCode(code);
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
                    this.mySqlConnection.handleException(e);
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
                    this.mySqlConnection.handleException(e);
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
                    this.mySqlConnection.handleException(e);
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
            final BindCodeTable t = this.table;

            if (t != null) {
                final int c;
                try {
                    c = this.cleanOutdated();
                } catch (SQLException e) {
                    this.table = null;
                    this.connection = null;
                    try {
                        t.close();
                    } catch (SQLException ignored) {
                    }

                    throw e;
                }

                this.table = null;
                this.connection = null;

                t.close();

                return c;
            }

            return -1;
        }
    }
}