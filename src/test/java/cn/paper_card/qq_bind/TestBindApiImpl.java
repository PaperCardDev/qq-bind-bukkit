package cn.paper_card.qq_bind;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.qq_bind.api.BindInfo;
import cn.paper_card.qq_bind.api.exception.AlreadyBindException;
import cn.paper_card.qq_bind.api.exception.QqHasBeenBindException;
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
            public @NotNull Connection getRawConnection() throws SQLException {
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
            public void close() {

            }

            @Override
            public void handleException(@NotNull SQLException e) throws SQLException {
                final Connection c = this.connection;
                this.connection = null;
                if (c != null) c.close();
            }

        };
    }


    @Test
    public void test() throws Exception {
        final DatabaseApi.MySqlConnection mySqlConnection = getMySqlConnection();
        final BindServiceImpl bindApi = new BindServiceImpl(mySqlConnection);


        // 测试添加绑定
        final BindInfo test = new BindInfo(new UUID(0, 0), "Test", 114514, "JustTest", System.currentTimeMillis());

        bindApi.addBind(test);

        // 测试查询
        {
            final BindInfo info = bindApi.queryByUuid(test.uuid());
//            Assert.assertNotNull(info);
            Assert.assertEquals(test, info);
        }

        // 测试查询
        {
            final BindInfo i2 = bindApi.queryByQq(test.qq());
//            Assert.assertNotNull(i2);
            Assert.assertEquals(test, i2);
        }


        // 测试删除
        final boolean deleted = bindApi.removeBind(test.uuid(), test.qq());

        Assert.assertTrue(deleted);

        // 删除后应该查不到
        {
            final BindInfo info = bindApi.queryByUuid(test.uuid());
            Assert.assertNull(info);
        }

        {
            final BindInfo info = bindApi.queryByQq(test.qq());
            Assert.assertNull(info);
        }

        bindApi.close();
    }

    @Test
    public void test2() throws AlreadyBindException, SQLException, QqHasBeenBindException {
        final DatabaseApi.MySqlConnection mySqlConnection = getMySqlConnection();
        final BindServiceImpl bindApi = new BindServiceImpl(mySqlConnection);

        // 测试一个UUID重复绑定的情况
        final BindInfo test = new BindInfo(new UUID(0, 0), "Test", 114514, "JustTest", System.currentTimeMillis());

        bindApi.addBind(test);

        Assert.assertThrows(AlreadyBindException.class, () -> bindApi.addBind(new BindInfo(new UUID(0, 0), "Test", 114515, "JustTest", System.currentTimeMillis())));

        bindApi.removeBind(test.uuid(), test.qq());
    }


    @Test
    public void test3() throws Exception {
        final DatabaseApi.MySqlConnection mySqlConnection = getMySqlConnection();
        final BindServiceImpl bindApi = new BindServiceImpl(mySqlConnection);

        // 测试一个QQ被多个绑定
        final BindInfo test = new BindInfo(new UUID(0, 0), "Test", 114514, "JustTest", System.currentTimeMillis());

        bindApi.addBind(test); // 应该成功

        // 异常
        Assert.assertThrows(QqHasBeenBindException.class, () -> bindApi.addBind(new BindInfo(new UUID(0, 1), "Test2", test.qq(), "JustTest", System.currentTimeMillis())));

        // 删除
        bindApi.removeBind(test.uuid(), test.qq());
    }
}
