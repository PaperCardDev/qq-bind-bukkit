package cn.paper_card.player_qq_bind;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class TestQqBindApiImpl {

    // 测试，生成绑定验证码
    @Test
    public void test1() throws Exception {
        final Logger logger = Logger.getLogger("Test");

        final QqBindApiImpl qqBindApi = new QqBindApiImpl(TestBindApiImpl.getMySqlConnection(), TestBindApiImpl.getMySqlConnection(), logger, new ConfigManagerTest());

        final String name = "Paper99";
        final UUID uuid = UUID.randomUUID();

        final boolean botOnline = new Random().nextBoolean();
        final QqBindApi.QqBot bot = botOnline ? (qq, message) -> logger.fine("发送群At消息 {qq: %d, msg: %s}".formatted(qq, message)) : null;

        { // 删除绑定
            final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByUuid(uuid);
            if (info != null) {
                final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
                Assert.assertTrue(deleted);
            }
        }

        final QqBindApi.PreLoginResponse response = qqBindApi.handlePreLogin(new QqBindApi.PreLoginRequest() {
            @Override
            public @NotNull String getName() {
                return name;
            }

            @Override
            public @NotNull UUID getUuid() {
                return uuid;
            }

            @Override
            public QqBindApi.QqBot getQqBot() {
                return bot;
            }

            @Override
            public QqBindApi.@Nullable AutoQqBind getAutoQqBind() {
                return null;
            }
        });

        final AsyncPlayerPreLoginEvent.Result result = response.result();

        Assert.assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, result);
        Assert.assertEquals(0, response.qq());

        final Component textComponent = response.kickMessage();
        Assert.assertNotNull(textComponent);


        for (Component child : textComponent.children()) {
            final TextComponent t2 = (TextComponent) child;
            System.out.print(t2.content());
        }
        System.out.println();

        final List<String> names = qqBindApi.getBindCodeApi().queryPlayerNames();
        Assert.assertTrue(names.contains("Paper99"));

        Assert.assertTrue(response.bindCode() > 0);

        {
            final QqBindApi.BindCodeInfo info = qqBindApi.getBindCodeApi().takeByCode(response.bindCode());
            Assert.assertNotNull(info);
            System.out.println(info);
        }

        qqBindApi.close();
    }

    // 测试自动绑定成功
    @Test
    public void test3() throws Exception {
        final Logger logger = Logger.getLogger("Test");

        final QqBindApiImpl qqBindApi = new QqBindApiImpl(TestBindApiImpl.getMySqlConnection(), TestBindApiImpl.getMySqlConnection(), logger, new ConfigManagerTest());

        final String name = "Paper99";
        final UUID uuid = UUID.randomUUID();

        { // 删除绑定
            final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByUuid(uuid);
            if (info != null) {
                final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
                Assert.assertTrue(deleted);
            }
        }

        final long testQq = 1398872296;

        { // 确保这个QQ没有被绑定
            final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByQq(testQq);
            if (info != null) {
                final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
                Assert.assertTrue(deleted);
            }
        }

        final boolean botOnline = new Random().nextBoolean();
        final QqBindApi.QqBot bot = botOnline ? (qq, message) -> logger.info("发送群At消息 {qq: %d, msg: %s}".formatted(qq, message)) : null;

        final QqBindApi.PreLoginResponse response = qqBindApi.handlePreLogin(new QqBindApi.PreLoginRequest() {
            @Override
            public @NotNull String getName() {
                return name;
            }

            @Override
            public @NotNull UUID getUuid() {
                return uuid;
            }

            @Override
            public QqBindApi.QqBot getQqBot() {
                return bot;
            }

            @Override
            public QqBindApi.AutoQqBind getAutoQqBind() {
                return (uuid1, name1) -> testQq;
            }
        });

        final AsyncPlayerPreLoginEvent.Result result = response.result();

        Assert.assertNotSame(AsyncPlayerPreLoginEvent.Result.ALLOWED, result);

        final Component textComponent = response.kickMessage();
        Assert.assertNotNull(textComponent);

        for (Component child : textComponent.children()) {
            final TextComponent t2 = (TextComponent) child;
            System.out.print(t2.content());
        }
        System.out.println();


        // 检查绑定情况
        {
            final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByQq(testQq);
            System.out.println(info);
            Assert.assertNotNull(info);
            Assert.assertEquals(uuid, info.uuid());
            Assert.assertEquals("Paper99", info.name());

            final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
            Assert.assertTrue(deleted);
        }


        qqBindApi.close();
    }

    // 测试发送验证码绑定
    @Test
    public void test2() throws Exception {
        final Logger logger = Logger.getLogger("Test");

        final QqBindApiImpl qqBindApi = new QqBindApiImpl(TestBindApiImpl.getMySqlConnection(), TestBindApiImpl.getMySqlConnection(), logger, new ConfigManagerTest());

        final String name = "Paper99";
        final UUID uuid = UUID.randomUUID();

        final boolean botOnline = new Random().nextBoolean();
        final QqBindApi.QqBot bot = botOnline ? (qq, message) -> logger.fine("发送群At消息 {qq: %d, msg: %s}".formatted(qq, message)) : null;

        { // 删除绑定
            final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByUuid(uuid);
            if (info != null) {
                final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
                Assert.assertTrue(deleted);
            }
        }

        final QqBindApi.PreLoginRequest request = new QqBindApi.PreLoginRequest() {
            @Override
            public @NotNull String getName() {
                return name;
            }

            @Override
            public @NotNull UUID getUuid() {
                return uuid;
            }

            @Override
            public QqBindApi.QqBot getQqBot() {
                return bot;
            }

            @Override
            public QqBindApi.@Nullable AutoQqBind getAutoQqBind() {
                return null;
            }
        };

        final QqBindApi.PreLoginResponse response = qqBindApi.handlePreLogin(request);

        final AsyncPlayerPreLoginEvent.Result result = response.result();

        Assert.assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, result);
        Assert.assertEquals(0, response.qq());

        final Component textComponent = response.kickMessage();
        Assert.assertNotNull(textComponent);


        for (Component child : textComponent.children()) {
            final TextComponent t2 = (TextComponent) child;
            System.out.print(t2.content());
        }
        System.out.println();

        final List<String> names = qqBindApi.getBindCodeApi().queryPlayerNames();
        Assert.assertTrue(names.contains("Paper99"));

        final int bindCode = response.bindCode();
        Assert.assertTrue(bindCode > 0);

        final long testQQ = 1398872296;

        assert qqBindApi.getBindCodeApi().getCodeCount() > 0;

        // 模拟发送验证码
        final List<String> strings = qqBindApi.onMainGroupMessage(testQQ, "%d".formatted(bindCode));
        Assert.assertNotNull(strings);
        for (String string : strings) {
            System.out.println(string);
        }

        // 检查是否绑定
        final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByUuid(uuid);
        Assert.assertNotNull(info);
        Assert.assertEquals(testQQ, info.qq());

        // 再次请求
        final QqBindApi.PreLoginResponse response1 = qqBindApi.handlePreLogin(request);
        Assert.assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, response1.result());
        Assert.assertEquals(testQQ, response1.qq());

        // 删除绑定
        final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
        Assert.assertTrue(deleted);

        qqBindApi.close();
    }

    @Test
    @Ignore
    public void test4() throws Exception {
        final Logger logger = Logger.getLogger("Test");
        final QqBindApiImpl qqBindApi = new QqBindApiImpl(TestBindApiImpl.getMySqlConnection(), TestBindApiImpl.getMySqlConnection(), logger, new ConfigManagerTest());

        final LinkedList<Integer> codes = new LinkedList<>();

        for (int i = 0; i < 100; ++i) {
            final String name = "Paper" + i;
            final UUID uuid = UUID.randomUUID();

            final boolean botOnline = new Random().nextBoolean();
            final QqBindApi.QqBot bot = botOnline ? (qq, message) -> logger.fine("发送群At消息 {qq: %d, msg: %s}".formatted(qq, message)) : null;

            { // 删除绑定
                final QqBindApi.BindInfo info = qqBindApi.getBindApi().queryByUuid(uuid);
                if (info != null) {
                    final boolean deleted = qqBindApi.getBindApi().deleteByUuidAndQq(info.uuid(), info.qq());
                    Assert.assertTrue(deleted);
                }
            }

            final QqBindApi.PreLoginRequest request = new QqBindApi.PreLoginRequest() {
                @Override
                public @NotNull String getName() {
                    return name;
                }

                @Override
                public @NotNull UUID getUuid() {
                    return uuid;
                }

                @Override
                public QqBindApi.QqBot getQqBot() {
                    return bot;
                }

                @Override
                public QqBindApi.@Nullable AutoQqBind getAutoQqBind() {
                    return null;
                }
            };

            final QqBindApi.PreLoginResponse response = qqBindApi.handlePreLogin(request);

            final AsyncPlayerPreLoginEvent.Result result = response.result();

            Assert.assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, result);
            Assert.assertEquals(0, response.qq());

            final Component textComponent = response.kickMessage();
            Assert.assertNotNull(textComponent);


            for (Component child : textComponent.children()) {
                final TextComponent t2 = (TextComponent) child;
                System.out.print(t2.content());
            }
            System.out.println();

            assert response.bindCode() > 0;

            codes.add(response.bindCode());
        }

        // 模拟发送消息
        for (final Integer code : codes) {
            final List<String> strings = qqBindApi.onMainGroupMessage(new Random().nextLong(9999999), code.toString());
            assert strings != null;

            for (String string : strings) {
                System.out.println(string);
            }
        }

        qqBindApi.close();
    }
}
