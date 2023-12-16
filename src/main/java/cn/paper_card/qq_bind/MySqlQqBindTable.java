package cn.paper_card.qq_bind;

import cn.paper_card.database.api.Util;
import cn.paper_card.qq_bind.api.BindInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

class MySqlQqBindTable {
    private static final String NAME = "qq_bind";

    private final @NotNull Connection connection;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementDeleteByUuidAndQq = null;

    private PreparedStatement statementQueryByUuid = null;

    private PreparedStatement statementQueryByQq = null;

    private PreparedStatement statementUpdateName = null;

    MySqlQqBindTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    uid1 BIGINT NOT NULL,
                    uid2 BIGINT NOT NULL,
                    name VARCHAR(64) NOT NULL,
                    qq BIGINT NOT NULL UNIQUE,
                    remark VARCHAR(128) NOT NULL,
                    time BIGINT NOT NULL,
                    PRIMARY KEY(uid1, uid2)
                )""".formatted(NAME));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, name, qq, remark, time) VALUES (?, ?, ?, ?, ?, ?)".formatted(NAME));
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementUpdateName() throws SQLException {
        if (this.statementUpdateName == null) {
            this.statementUpdateName = this.connection.prepareStatement
                    ("UPDATE %s SET name=? WHERE qq=? AND uid1=? AND uid2=? LIMIT 1".formatted(NAME));
        }
        return this.statementUpdateName;
    }

    private @NotNull PreparedStatement getStatementDeleteByUuidAndQq() throws SQLException {
        if (this.statementDeleteByUuidAndQq == null) {
            this.statementDeleteByUuidAndQq = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE uid1=? AND uid2=? AND qq=? LIMIT 1".formatted(NAME));
        }
        return this.statementDeleteByUuidAndQq;
    }

    private @NotNull PreparedStatement getStatementQueryByUuid() throws SQLException {
        if (this.statementQueryByUuid == null) {
            this.statementQueryByUuid = this.connection.prepareStatement
                    ("SELECT uid1, uid2, name, qq, remark, time FROM %s WHERE uid1=? AND uid2=? LIMIT 1 OFFSET 0".formatted(NAME));
        }
        return this.statementQueryByUuid;
    }

    private @NotNull PreparedStatement getStatementQueryByQq() throws SQLException {
        if (this.statementQueryByQq == null) {
            this.statementQueryByQq = this.connection.prepareStatement
                    ("SELECT uid1, uid2, name, qq, remark, time FROM %s WHERE qq=? LIMIT 1 OFFSET 0".formatted(NAME));
        }
        return this.statementQueryByQq;
    }

    private BindInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final long uid1 = resultSet.getLong(1);
        final long uid2 = resultSet.getLong(2);
        final String name = resultSet.getString(3);

        final long qq = resultSet.getLong(4);
        final String remark = resultSet.getString(5);
        final long time = resultSet.getLong(6);

        return new BindInfo(new UUID(uid1, uid2), name, qq, remark, time);
    }

    private @Nullable BindInfo parseOne(@NotNull ResultSet resultSet) throws SQLException {

        final BindInfo info;

        try {
            if (resultSet.next()) {
                info = this.parseRow(resultSet);
            } else info = null;

            if (resultSet.next()) throw new SQLException("不应该还有数据！");
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return info;
    }

    int insert(@NotNull BindInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setLong(1, info.uuid().getMostSignificantBits());
        ps.setLong(2, info.uuid().getLeastSignificantBits());
        ps.setString(3, info.name());
        ps.setLong(4, info.qq());
        ps.setString(5, info.remark());
        ps.setLong(6, info.time());
        return ps.executeUpdate();
    }

    int deleteByUuidAndQq(@NotNull UUID uuid, long qq) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteByUuidAndQq();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        ps.setLong(3, qq);
        return ps.executeUpdate();
    }

    @Nullable BindInfo queryByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByUuid();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    @Nullable BindInfo queryByQq(long qq) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByQq();
        ps.setLong(1, qq);
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    int updateName(@NotNull UUID uuid, long qq, @NotNull String name) throws SQLException {
        final PreparedStatement ps = this.getStatementUpdateName();
        ps.setString(1, name);
        ps.setLong(2, qq);
        ps.setLong(3, uuid.getMostSignificantBits());
        ps.setLong(4, uuid.getLeastSignificantBits());
        return ps.executeUpdate();
    }
}
