package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

public class TestBindApiImpl {


    public static @NotNull DatabaseApi.MySqlConnection getMySqlConnection() {
        return new DatabaseApi.MySqlConnection() {

            private long lastUse = 0;
            private int count = 0;

            private Connection connection = null;

            @Override
            public long getLastUseTime() {
                return this.lastUse;
            }

            @Override
            public void setLastUseTime() {
                this.lastUse = System.currentTimeMillis();
            }

            @Override
            public @NotNull Connection getRowConnection() throws SQLException {
                if (this.connection == null) {
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                    } catch (ClassNotFoundException e) {
                        throw new SQLException(e);
                    }
                    //3、获取数据库的连接对象
                    this.connection = DriverManager.getConnection("jdbc:mysql://localhost/test", "root", "qwer4321");
                    ++this.count;
                    return this.connection;
                }

                return this.connection;
            }

            @Override
            public int getConnectCount() {
                return this.count;
            }

            @Override
            public void testConnection() {
            }

            @Override
            public void checkClosedException(@NotNull SQLException e) throws SQLException {
                final Connection c = this.connection;
                this.connection = null;
                if (c != null) c.close();
            }
        };
    }


    @Test
    public void test() throws Exception {
        final DatabaseApi.MySqlConnection mySqlConnection = getMySqlConnection();
        final BindApiImpl bindApi = new BindApiImpl(mySqlConnection);


        // 测试添加绑定
        final QqBindApi.BindInfo test = new QqBindApi.BindInfo(new UUID(0, 0), "Test", 114514, "JustTest", System.currentTimeMillis());

        bindApi.addBind(test);

        // 测试查询
        {
            final QqBindApi.BindInfo info = bindApi.queryByUuid(test.uuid());
//            Assert.assertNotNull(info);
            Assert.assertEquals(test, info);
        }

        // 测试查询
        {
            final QqBindApi.BindInfo i2 = bindApi.queryByQq(test.qq());
//            Assert.assertNotNull(i2);
            Assert.assertEquals(test, i2);
        }


        // 测试删除
        final boolean deleted = bindApi.deleteByUuidAndQq(test.uuid(), test.qq());

        Assert.assertTrue(deleted);

        // 删除后应该查不到
        {
            final QqBindApi.BindInfo info = bindApi.queryByUuid(test.uuid());
            Assert.assertNull(info);
        }

        {
            final QqBindApi.BindInfo info = bindApi.queryByQq(test.qq());
            Assert.assertNull(info);
        }

        bindApi.close();
    }

    @Test
    public void test2() throws QqBindApi.BindApi.QqHasBeenBindedException, SQLException, QqBindApi.BindApi.AreadyBindException {
        final DatabaseApi.MySqlConnection mySqlConnection = getMySqlConnection();
        final BindApiImpl bindApi = new BindApiImpl(mySqlConnection);

        // 测试一个UUID重复绑定的情况
        final QqBindApi.BindInfo test = new QqBindApi.BindInfo(new UUID(0, 0), "Test", 114514, "JustTest", System.currentTimeMillis());

        bindApi.addBind(test);

        Assert.assertThrows(QqBindApi.BindApi.AreadyBindException.class, () -> bindApi.addBind(new QqBindApi.BindInfo(new UUID(0, 0), "Test", 114515, "JustTest", System.currentTimeMillis())));

        bindApi.deleteByUuidAndQq(test.uuid(), test.qq());
    }


    @Test
    public void test3() throws Exception {
        final DatabaseApi.MySqlConnection mySqlConnection = getMySqlConnection();
        final BindApiImpl bindApi = new BindApiImpl(mySqlConnection);

        // 测试一个QQ被多个绑定
        final QqBindApi.BindInfo test = new QqBindApi.BindInfo(new UUID(0, 0), "Test", 114514, "JustTest", System.currentTimeMillis());

        bindApi.addBind(test); // 应该成功

        // 异常
        Assert.assertThrows(QqBindApi.BindApi.QqHasBeenBindedException.class, () -> bindApi.addBind(new QqBindApi.BindInfo(new UUID(0, 1), "Test2", test.qq(), "JustTest", System.currentTimeMillis())));

        // 删除
        bindApi.deleteByUuidAndQq(test.uuid(), test.qq());
    }
}
