package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

class BindCodeApiImpl implements QqBindApi.BindCodeApi {
    BindCodeApiImpl(@NotNull PlayerQqBind plugin) {
        this.plugin = plugin;
    }

    private BindCodeTable table = null;
    private DatabaseConnection connection = null;

    private final @NotNull PlayerQqBind plugin;

    private @NotNull DatabaseConnection getConnection() throws Exception {
        if (this.connection == null) {
            this.connection = this.plugin.getDatabaseApi().connectUnimportant();
        }
        return this.connection;
    }

    private @NotNull BindCodeTable getTable() throws Exception {
        if (this.table == null) {
            this.table = new BindCodeTable(this.getConnection().getConnection());
        }
        return this.table;
    }

    private int randomCode() {
        final int min = 1;
        final int max = 999999;
        return new Random().nextInt(max - min + 1) + min;
    }


    @Override
    public int createCode(@NotNull UUID uuid, @NotNull String name) throws Exception {
        final int code = this.randomCode();
        final long time = System.currentTimeMillis();
        synchronized (this) {
            final BindCodeTable t = this.getTable();

            // 为了防止验证码重复
            final List<QqBindApi.BindCodeInfo> list = t.queryByCode(code);
            if (!list.isEmpty()) return 0;

            final QqBindApi.BindCodeInfo info = new QqBindApi.BindCodeInfo(code, name, uuid, time);
            final int updated = t.updateByUuid(info);
            if (updated == 0) {
                final int inserted = t.insert(info);
                if (inserted != 1) throw new Exception("插入了%d条数据！".formatted(inserted));
                return code;
            }
            if (updated == 1) return code;
            throw new Exception("根据一个UUID更新了多条数据！");
        }
    }

    // 取出一个绑定验证码
    @Override
    public @Nullable QqBindApi.BindCodeInfo takeByCode(int code) throws Exception {
        synchronized (this) {
            final BindCodeTable t = this.getTable();
            final List<QqBindApi.BindCodeInfo> list = t.queryByCode(code);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) {
                final int deleted = t.deleteByCode(code);
                if (deleted != 1) throw new Exception("删除了%d条数据！".formatted(deleted));

                final QqBindApi.BindCodeInfo info = list.get(0);

                // 判断验证码是否过期
                final long delta = System.currentTimeMillis() - info.time();
                if (delta > 2 * 60 * 1000L) return null;
                return info;
            }
            throw new Exception("根据一个验证码查询到%d条数据！".formatted(size));
        }
    }

    void close() {
        synchronized (this) {
            if (this.table != null) {
                try {
                    this.table.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.table = null;
            }

            if (this.connection != null) {
                try {
                    this.connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.connection = null;
            }
        }
    }


    static class BindCodeTable {

        private final PreparedStatement statementInsert;

        private final PreparedStatement statementUpdateByUuid;

        private final PreparedStatement statementQueryByCode;

        private final PreparedStatement statementDeleteByCode;

        private final static String NAME = "qq_bind_code";


        BindCodeTable(@NotNull Connection connection) throws SQLException {
            this.createTable(connection);

            try {
                this.statementInsert = connection.prepareStatement("""
                        INSERT INTO %s (code, uid1, uid2, name, time) VALUES (?, ?, ?, ?, ?)
                        """.formatted(NAME)
                );

                this.statementUpdateByUuid = connection.prepareStatement("""
                        UPDATE %s SET code=?, name=?, time=? WHERE uid1=? AND uid2=?
                        """.formatted(NAME)
                );

                this.statementQueryByCode = connection.prepareStatement
                        ("SELECT code, uid1, uid2, name, time FROM %s WHERE code=?".formatted(NAME));

                this.statementDeleteByCode = connection.prepareStatement
                        ("DELETE FROM %s WHERE code=?".formatted(NAME));

            } catch (SQLException e) {
                try {
                    this.close();
                } catch (SQLException ignored) {
                }

                throw e;
            }
        }

        private void createTable(@NotNull Connection connection) throws SQLException {
            final String sql2 = """
                    CREATE TABLE IF NOT EXISTS %s (
                        code    INTEGER NOT NULL,
                        uid1    INTEGER NOT NULL,
                        uid2    INTEGER NOT NULL,
                        name    VARCHAR(64) NOT NULL,
                        time    INTEGER NOT NULL
                    )
                    """.formatted(NAME);
            DatabaseConnection.createTable(connection, sql2);
        }

        void close() throws SQLException {
            DatabaseConnection.closeAllStatements(this.getClass(), this);
        }

        int insert(@NotNull QqBindApi.BindCodeInfo info) throws SQLException {
            final PreparedStatement ps = this.statementInsert;
            ps.setInt(1, info.code());
            ps.setLong(2, info.uuid().getMostSignificantBits());
            ps.setLong(3, info.uuid().getLeastSignificantBits());
            ps.setString(4, info.name());
            ps.setLong(5, info.time());
            return ps.executeUpdate();
        }

        int updateByUuid(@NotNull QqBindApi.BindCodeInfo info) throws SQLException {
            final PreparedStatement ps = this.statementUpdateByUuid;
            // UPDATE %s SET code=?, name=?, time=? WHERE uid1=? AND uid2=?
            ps.setInt(1, info.code());
            ps.setString(2, info.name());
            ps.setLong(3, info.time());
            ps.setLong(4, info.uuid().getMostSignificantBits());
            ps.setLong(5, info.uuid().getLeastSignificantBits());
            return ps.executeUpdate();
        }

        private LinkedList<QqBindApi.BindCodeInfo> parse(@NotNull ResultSet resultSet) throws SQLException {
            final LinkedList<QqBindApi.BindCodeInfo> list = new LinkedList<>();
            try {
                // "SELECT code, uid1, uid2, name, time FROM %s WHERE code=?"
                while (resultSet.next()) {
                    final int code = resultSet.getInt(1);
                    final long uid1 = resultSet.getLong(2);
                    final long uid2 = resultSet.getLong(3);
                    final String name = resultSet.getString(4);
                    final long time = resultSet.getLong(5);
                    list.add(new QqBindApi.BindCodeInfo(code, name, new UUID(uid1, uid2), time));
                }
            } catch (SQLException e) {
                try {
                    resultSet.close();
                } catch (SQLException ignored) {
                }
                throw e;
            }
            resultSet.close();

            return list;
        }

        @NotNull List<QqBindApi.BindCodeInfo> queryByCode(int code) throws SQLException {
            final PreparedStatement ps = this.statementQueryByCode;
            ps.setInt(1, code);
            final ResultSet resultSet = ps.executeQuery();
            return this.parse(resultSet);
        }

        int deleteByCode(int code) throws SQLException {
            final PreparedStatement ps = this.statementDeleteByCode;
            ps.setInt(1, code);
            return ps.executeUpdate();
        }
    }
}
