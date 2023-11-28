package cn.paper_card.qq_bind;

import cn.paper_card.database.api.Util;
import cn.paper_card.qq_bind.api.BindCodeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class BindCodeTable {

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementUpdateByUuid = null;

    private PreparedStatement statementQueryByCode = null;

    private PreparedStatement statementDeleteByCode = null;


    private PreparedStatement statementDeleteTimeBefore = null;

    private PreparedStatement statementQueryNames = null;

    private PreparedStatement statementQueryCount = null;

    private final static String NAME = "qq_bind_code";
    private final @NotNull Connection connection;


    BindCodeTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.createTable();
    }

    private void createTable() throws SQLException {
        final String sql2 = """
                CREATE TABLE IF NOT EXISTS %s (
                    code    INT NOT NULL UNIQUE,
                    uid1    BIGINT NOT NULL,
                    uid2    BIGINT NOT NULL,
                    name    VARCHAR(64) NOT NULL,
                    time    BIGINT NOT NULL,
                    PRIMARY KEY(uid1, uid2)
                )""".formatted(NAME);

        Util.executeSQL(this.connection, sql2);
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement("""
                    INSERT INTO %s (code, uid1, uid2, name, time) VALUES (?, ?, ?, ?, ?)
                    """.formatted(NAME)
            );
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementUpdateByUuid() throws SQLException {
        if (this.statementUpdateByUuid == null) {
            this.statementUpdateByUuid = connection.prepareStatement("""
                    UPDATE %s SET code=?, name=?, time=? WHERE uid1=? AND uid2=? LIMIT 1
                    """.formatted(NAME)
            );
        }
        return this.statementUpdateByUuid;
    }

    private @NotNull PreparedStatement getStatementQueryByCode() throws SQLException {
        if (this.statementQueryByCode == null) {
            this.statementQueryByCode = this.connection.prepareStatement
                    ("SELECT code, uid1, uid2, name, time FROM %s WHERE code=? LIMIT 1".formatted(NAME));
        }
        return this.statementQueryByCode;
    }

    private @NotNull PreparedStatement getStatementDeleteByCode() throws SQLException {
        if (this.statementDeleteByCode == null) {
            this.statementDeleteByCode = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE code=? LIMIT 1".formatted(NAME));
        }
        return this.statementDeleteByCode;
    }

    private @NotNull PreparedStatement getStatementDeleteTimeBefore() throws SQLException {
        if (this.statementDeleteTimeBefore == null) {
            this.statementDeleteTimeBefore = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE time<?".formatted(NAME));
        }
        return this.statementDeleteTimeBefore;
    }

    private @NotNull PreparedStatement getStatementQueryNames() throws SQLException {
        if (this.statementQueryNames == null) {
            this.statementQueryNames = this.connection.prepareStatement
                    ("SELECT name FROM %s".formatted(NAME));
        }
        return this.statementQueryNames;
    }

    private @NotNull PreparedStatement getStatementQueryCount() throws SQLException {
        if (this.statementQueryCount == null) {
            this.statementQueryCount = this.connection.prepareStatement
                    ("SELECT count(*) FROM %s".formatted(NAME));
        }
        return this.statementQueryCount;
    }

    int insert(@NotNull BindCodeInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();

        ps.setInt(1, info.code());
        ps.setLong(2, info.uuid().getMostSignificantBits());
        ps.setLong(3, info.uuid().getLeastSignificantBits());
        ps.setString(4, info.name());
        ps.setLong(5, info.time());
        return ps.executeUpdate();
    }

    int updateByUuid(@NotNull BindCodeInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementUpdateByUuid();
        // UPDATE %s SET code=?, name=?, time=? WHERE uid1=? AND uid2=?
        ps.setInt(1, info.code());
        ps.setString(2, info.name());
        ps.setLong(3, info.time());
        ps.setLong(4, info.uuid().getMostSignificantBits());
        ps.setLong(5, info.uuid().getLeastSignificantBits());
        return ps.executeUpdate();
    }

    private @NotNull BindCodeInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final int code = resultSet.getInt(1);
        final long uid1 = resultSet.getLong(2);
        final long uid2 = resultSet.getLong(3);
        final String name = resultSet.getString(4);
        final long time = resultSet.getLong(5);
        return new BindCodeInfo(code, name, new UUID(uid1, uid2), time);
    }

    private @Nullable BindCodeInfo parseOne(@NotNull ResultSet resultSet) throws SQLException {

        final BindCodeInfo info;
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

    @Nullable BindCodeInfo queryByCode(int code) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByCode();
        ps.setInt(1, code);
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    int deleteByCode(int code) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteByCode();
        ps.setInt(1, code);
        return ps.executeUpdate();
    }

    int deleteTimeBefore(long time) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteTimeBefore();
        ps.setLong(1, time);
        return ps.executeUpdate();
    }

    int queryCount() throws SQLException {
        final ResultSet resultSet = this.getStatementQueryCount().executeQuery();

        final int count;
        try {
            if (resultSet.next()) {
                count = resultSet.getInt(1);
            } else throw new SQLException("不应该没有数据！");

            if (resultSet.next()) throw new SQLException("不应该还有数据！");

        } catch (SQLException e) {

            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return count;
    }

    @NotNull List<String> queryNames() throws SQLException {
        final ResultSet resultSet = this.getStatementQueryNames().executeQuery();

        final List<String> names = new LinkedList<>();
        try {
            while (resultSet.next()) {
                final String name = resultSet.getString(1);
                names.add(name);
            }
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return names;
    }
}
