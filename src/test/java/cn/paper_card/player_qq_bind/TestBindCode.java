package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class TestBindCode {

    @Test
    public void test1() throws QqBindApi.BindCodeApi.DuplicatedCode, SQLException {
        final DatabaseApi.MySqlConnection mySqlConnection = TestBindApiImpl.getMySqlConnection();
        final BindCodeApiImpl bindCodeApi = new BindCodeApiImpl(mySqlConnection);

        // 测试生成
        final int code = bindCodeApi.createCode(new UUID(0, 0), "Test");

        // 测试查询
        {
            final List<String> names = bindCodeApi.queryPlayerNames();
            Assert.assertFalse(names.isEmpty());
            Assert.assertTrue(names.contains("Test"));
        }

        // 测试取出
        final QqBindApi.BindCodeInfo info = bindCodeApi.takeByCode(code);
        Assert.assertNotNull(info);
        Assert.assertEquals(new UUID(0, 0), info.uuid());
        Assert.assertEquals("Test", info.name());
        Assert.assertEquals(code, info.code());

        bindCodeApi.close();
//        Assert.assertEquals(0, clean);
    }

    @Test
    public void test2() throws SQLException, QqBindApi.BindCodeApi.DuplicatedCode {
        final DatabaseApi.MySqlConnection mySqlConnection = TestBindApiImpl.getMySqlConnection();
        final BindCodeApiImpl bindCodeApi = new BindCodeApiImpl(mySqlConnection);


        final int code1 = bindCodeApi.createCode(new UUID(0, 0), "Test");
        final int code2 = bindCodeApi.createCode(new UUID(0, 0), "Test2");

        Assert.assertNotSame(code1, code2);

        final QqBindApi.BindCodeInfo info = bindCodeApi.takeByCode(code1);
        Assert.assertNull(info);

        final QqBindApi.BindCodeInfo info1 = bindCodeApi.takeByCode(code2);
        Assert.assertNotNull(info1);
        Assert.assertEquals(code2, info1.code());
        Assert.assertEquals("Test2", info1.name());
        Assert.assertEquals(new UUID(0, 0), info1.uuid());

        final int clean = bindCodeApi.close();
        Assert.assertEquals(0, clean);
    }

    @Test
    @Ignore
    public void test3() throws SQLException {
        final DatabaseApi.MySqlConnection mySqlConnection = TestBindApiImpl.getMySqlConnection();
        final BindCodeApiImpl bindCodeApi = new BindCodeApiImpl(mySqlConnection);

        Assert.assertThrows(QqBindApi.BindCodeApi.DuplicatedCode.class, () -> {
            for (int i = 1; i <= 999999; ++i) {
                final int code = bindCodeApi.createCode(new UUID(0, i), "Test" + i);
                System.out.println(code);
            }
        });

        bindCodeApi.close();
    }

    @Test
    @Ignore
    public void test4() throws QqBindApi.BindCodeApi.DuplicatedCode, SQLException, InterruptedException {
        final DatabaseApi.MySqlConnection mySqlConnection = TestBindApiImpl.getMySqlConnection();
        final BindCodeApiImpl bindCodeApi = new BindCodeApiImpl(mySqlConnection);

        final int code = bindCodeApi.createCode(new UUID(0, 0), "Test");

        Thread.sleep(bindCodeApi.getMaxAliveTime() + 2000);

        final QqBindApi.BindCodeInfo info = bindCodeApi.takeByCode(code);
        Assert.assertNull(info);

        bindCodeApi.close();
    }
}
