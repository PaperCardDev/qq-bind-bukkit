package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseConnection;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class QqBindTable {
    private final static String TABLE_NAME = "qq_bind";


    private final PreparedStatement statementInsert;
    private final PreparedStatement statementUpdateByUuid;

    private final PreparedStatement statementQueryByUuid;
    private final PreparedStatement statementQueryByQq;


    QqBindTable(@NotNull Connection connection) throws SQLException {

        this.createTable(connection);

        try {
            this.statementInsert = connection.prepareStatement("""
                    INSERT INTO %s (uid1, uid2, qq) VALUES (?, ?, ?)
                    """.formatted(TABLE_NAME)
            );

            this.statementUpdateByUuid = connection.prepareStatement("""
                    UPDATE %s SET qq=? WHERE uid1=? AND uid2=?
                    """.formatted(TABLE_NAME)
            );

            this.statementQueryByUuid = connection.prepareStatement("""
                    SELECT uid1, uid2, qq FROM %s WHERE uid1=? AND uid2=?
                    """.formatted(TABLE_NAME)
            );

            this.statementQueryByQq = connection.prepareStatement("""
                    SELECT uid1, uid2, qq FROM %s WHERE qq=?
                    """.formatted(TABLE_NAME)
            );


        } catch (SQLException e) {
            try {
                this.close();
            } catch (SQLException ignored) {
            }

            throw e;
        }
    }

    private void createTable(@NotNull Connection connection) throws SQLException {
        final String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                    uid1    INTEGER NOT NULL,
                    uid2    INTEGER NOT NULL,
                    qq      INTEGER NOT NULL
                )
                """.formatted(TABLE_NAME);
        DatabaseConnection.createTable(connection, sql);
    }

    void close() throws SQLException {
        DatabaseConnection.closeAllStatements(this.getClass(), this);
    }

    int insert(@NotNull QqBindApi.BindInfo info) throws SQLException {
        final PreparedStatement ps = this.statementInsert;
        ps.setLong(1, info.uuid().getMostSignificantBits());
        ps.setLong(2, info.uuid().getLeastSignificantBits());
        ps.setLong(3, info.qq());
        return ps.executeUpdate();
    }

    int updateQqByUuid(@NotNull UUID uuid, long qq) throws SQLException {
        final PreparedStatement ps = this.statementUpdateByUuid;
        ps.setLong(1, qq);
        ps.setLong(2, uuid.getMostSignificantBits());
        ps.setLong(3, uuid.getLeastSignificantBits());
        return ps.executeUpdate();
    }

    private @NotNull List<QqBindApi.BindInfo> parseAll(@NotNull ResultSet resultSet) throws SQLException {
        final List<QqBindApi.BindInfo> list = new LinkedList<>();

        try {
            while (resultSet.next()) {
                final long uid1 = resultSet.getLong(1);
                final long uid2 = resultSet.getLong(2);
                final long qq = resultSet.getLong(3);
                final QqBindApi.BindInfo bindInfo = new QqBindApi.BindInfo(new UUID(uid1, uid2), qq);
                list.add(bindInfo);
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

    @NotNull List<QqBindApi.BindInfo> queryByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.statementQueryByUuid;
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        final ResultSet resultSet = ps.executeQuery();
        return this.parseAll(resultSet);
    }

    @NotNull List<QqBindApi.BindInfo> queryByQq(long qq) throws SQLException {
        final PreparedStatement ps = this.statementQueryByQq;
        ps.setLong(1, qq);
        final ResultSet resultSet = ps.executeQuery();
        return this.parseAll(resultSet);
    }

}