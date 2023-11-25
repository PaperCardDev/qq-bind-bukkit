package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

class QqBindApiImpl implements QqBindApi {
    private final BindApiImpl bindApi;
    private final BindCodeApiImpl bindCodeApi;

    private final @NotNull Logger logger;

    private long groupId = 0;

    QqBindApiImpl(@NotNull DatabaseApi.MySqlConnection connection1, @NotNull DatabaseApi.MySqlConnection connection2, @NotNull Logger logger) {
        this.bindApi = new BindApiImpl(connection1);
        this.bindCodeApi = new BindCodeApiImpl(connection2);
        this.logger = logger;
    }

    @Override
    public @NotNull BindApi getBindApi() {
        return this.bindApi;
    }

    @Override
    public @NotNull BindCodeApi getBindCodeApi() {
        return this.bindCodeApi;
    }


    void close() {
        this.setGroupId(this.getGroupId());

        try {
            this.bindApi.close();
        } catch (SQLException e) {
            this.handleException(e);
        }

        try {
            this.bindCodeApi.close();
        } catch (SQLException e) {
            this.handleException(e);
        }
    }

    private @NotNull Logger getLogger() {
        return this.logger;
    }

    private @NotNull PreLoginResponse kickWhenException(@NotNull Exception e) {
        this.handleException(e);

        final TextComponent.Builder text = Component.text();

        text.append(Component.text("[ QQ绑定 | 错误 ]").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        return new PreLoginResponseImpl(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text.build(), -1);
    }

    void handleException(@NotNull Exception e) {
//        e.printStackTrace();
        this.logger.throwing(this.getClass().getSimpleName(), "handleException", e);
    }


    @NotNull PreLoginRequest createRequest(@NotNull AsyncPlayerPreLoginEvent event) {
        return new PreLoginRequest() {
            @Override
            public @NotNull String getName() {
                return event.getName();
            }

            @Override
            public @NotNull UUID getUuid() {
                return event.getUniqueId();
            }

            @Override
            public @Nullable QqBot getQqBot() {
                return null;
            }

            @Override
            public @Nullable AutoQqBind getAutoQqBind() {
                return null;
            }
        };
    }

    @NotNull String toReadableTime(long ms) {
        final long minutes = ms / (60 * 1000L);
        ms %= 60 * 1000L;
        final long seconds = ms / 1000L;

        final StringBuilder stringBuilder = new StringBuilder();

        if (minutes != 0) {
            stringBuilder.append(minutes);
            stringBuilder.append("分钟");
        }

        if (seconds != 0) {
            stringBuilder.append(minutes);
            stringBuilder.append('秒');
        }

        final String string = stringBuilder.toString();

        if (string.isEmpty()) return "0";

        return string;
    }

    private @Nullable PreLoginResponse tryAutoBind(@NotNull PreLoginRequest request) {

        final AutoQqBind autoQqBind = request.getAutoQqBind();
        if (autoQqBind == null) return null;

        final long qq;
        try {
            qq = autoQqBind.findPossibleQq(request.getUuid(), request.getName());
        } catch (Exception e) {
            return this.kickWhenException(e);
        }

        // 无法自动绑定
        if (qq == 0) return null;

        try {
            this.getBindApi().addBind(new BindInfo(request.getUuid(), request.getName(), qq,
                    "自动绑定", System.currentTimeMillis()));

        } catch (Exception e) {
            return this.kickWhenException(e);
        }

        getLogger().info("自动添加了QQ绑定：{name: %s, qq: %d}".formatted(request.getName(), qq));

        // 在群里发送消息通知
        final QqBot qqBot = request.getQqBot();

        if (qqBot != null) {

            qqBot.sendAtMessage(qq, """
                    已为你自动添加了QQ绑定：
                    游戏名：%s
                    如果这不是你，请及时联系管理员""".formatted(request.getName()));
        }

        final TextComponent.Builder text = Component.text();

        text.append(Component.text("[ QQ绑定 | 自动绑定 ]").color(NamedTextColor.AQUA));

        text.appendNewline();
        text.append(Component.text("已为你自动添加QQ绑定").color(NamedTextColor.GREEN));

        text.appendNewline();
        text.append(Component.text("游戏角色：").color(NamedTextColor.GREEN));
        text.append(Component.text(request.getName()).color(NamedTextColor.LIGHT_PURPLE));
        text.append(Component.text(" (%s)".formatted(request.getUuid().toString())).color(NamedTextColor.GRAY));

        text.appendNewline();
        text.append(Component.text("QQ：").color(NamedTextColor.GREEN));
        text.append(Component.text(qq).color(NamedTextColor.GOLD));

        text.appendNewline();
        text.append(Component.text("如果这不是你的QQ号，请联系管理员").color(NamedTextColor.YELLOW));

        return new PreLoginResponseImpl(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text.build(), 0);
    }

    @Override
    public @NotNull PreLoginResponse handlePreLogin(@NotNull PreLoginRequest request) {

        final BindInfo bindInfo;

        // 查询绑定信息
        try {
            bindInfo = this.getBindApi().queryByUuid(request.getUuid());
        } catch (Exception e) {
            return this.kickWhenException(e);
        }

        // 已经绑定了QQ
        if (bindInfo != null) {
            return new PreLoginResponseImpl(AsyncPlayerPreLoginEvent.Result.ALLOWED, null, bindInfo.qq());
        }

        // 没有绑定QQ

        { // 尝试自动绑定
            final PreLoginResponse response = this.tryAutoBind(request);
            if (response != null) return response;
        }

        // 无法自动绑定
        final int code;

        try {
            code = this.getBindCodeApi().createCode(request.getUuid(), request.getName());
        } catch (Exception e) {
            return this.kickWhenException(e);
        }

        final TextComponent.Builder text = Component.text();
        text.append(Component.text("[ QQ绑定 ]").color(NamedTextColor.AQUA));

        text.append(Component.newline());
        text.append(Component.text("QQ绑定验证码：").color(NamedTextColor.GREEN));
        text.append(Component.text(code).color(NamedTextColor.GOLD).decorate(TextDecoration.UNDERLINED));
        text.append(Component.text(" (有效时间：%s)".formatted(this.toReadableTime(this.getBindCodeApi().getMaxAliveTime())))
                .color(NamedTextColor.YELLOW));

        final long gid = this.getGroupId();
        text.append(Component.newline());
        if (gid > 0) {
            text.append(Component.text("请在我们的QQ群[").color(NamedTextColor.GREEN));
            text.append(Component.text(gid).color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
            text.append(Component.text("]里直接发送该数字验证码").color(NamedTextColor.GREEN));
        } else {
            text.append(Component.text("请在我们的QQ群里直接发送该数字验证码").color(NamedTextColor.GREEN));
        }

        if (request.getQqBot() != null) {
            text.append(Component.newline());
            text.append(Component.text("当前QQ机器人在线，会自动处理你发送的验证码").color(NamedTextColor.GREEN));
        } else {
            text.append(Component.newline());
            text.append(Component.text("当前QQ机器人不在线，请等待机器人修复").color(NamedTextColor.YELLOW));
        }

        text.appendNewline();
        text.append(Component.text("游戏角色：%s (%s)".formatted(request.getName(), request.getUuid().toString()))
                .color(NamedTextColor.GRAY));

        return new PreLoginResponseImpl(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, text.build(), 0, code);
    }


    private void appendWaitingNamesForMsg(@NotNull StringBuilder s, @NotNull List<String> names) {
        if (names.isEmpty()) return;

        s.append("\n----\n");
        s.append("继续等待玩家验证：");
        for (final String name : names) {
            s.append(" [%s]".formatted(name));
        }
    }

    @Override
    public @Nullable List<String> onMainGroupMessage(long qq, @NotNull String message) {

        // 没有任何一个账号等待绑定
        final int codeCount;

        codeCount = this.getBindCodeApi().getCodeCount();

        if (codeCount <= 0) return null;

        // 解析验证码
        final int code;
        try {
            code = Integer.parseInt(message);
        } catch (NumberFormatException ignored) {
            return null;
        }

        final LinkedList<String> reply = new LinkedList<>();

        // 清理过期验证码
        final int deleted;
        try {
            deleted = this.getBindCodeApi().cleanOutdated();
        } catch (Exception e) {
            this.handleException(e);
            reply.add(e.toString());
            return reply;
        }

        if (deleted > 0) {
            this.getLogger().info("清理了%d个过期的验证码".formatted(deleted));
        }

        // 取出验证码信息
        final BindCodeInfo bindCodeInfo;
        try {
            bindCodeInfo = this.getBindCodeApi().takeByCode(code);
        } catch (Exception e) {
            this.handleException(e);
            reply.add(e.toString());
            return reply;
        }

        // 查询还有谁
        final List<String> names;

        try {
            names = this.getBindCodeApi().queryPlayerNames();
        } catch (Exception e) {
            this.handleException(e);
            reply.add(e.toString());
            return reply;
        }

        if (bindCodeInfo == null) {
            final StringBuilder s = new StringBuilder();
            s.append("不存在或已过期失效的QQ绑定验证码：");
            s.append(code);
            s.append('\n');

            s.append("请重新获取新的验证码");

            this.appendWaitingNamesForMsg(s, names);

            reply.add(s.toString());
            return reply;
        }

        // 检查QQ绑定
        {
            final BindInfo bindInfo;

            try {
                bindInfo = this.getBindApi().queryByQq(qq);
            } catch (Exception e) {
                this.handleException(e);
                reply.add(e.toString());
                return reply;
            }

            // 已经绑定，不处理
            if (bindInfo != null) {

                final StringBuilder s = new StringBuilder();
                s.append("你的QQ已经绑定了一个正版号，不能再绑定其它正版号\n");
                s.append("你的游戏名: %s\n".formatted(bindInfo.name()));
                s.append("如需解绑，请联系管理员");
                this.appendWaitingNamesForMsg(s, names);

                reply.add(s.toString());
                return reply;
            }
        }


        // 添加绑定

        try {
            this.getBindApi().addBind(new BindInfo(bindCodeInfo.uuid(), bindCodeInfo.name(), qq, "验证码绑定", System.currentTimeMillis()));
        } catch (Exception e) {
            this.handleException(e);
            reply.add(e.toString());
            return reply;
        }

        this.getLogger().info("添加QQ绑定 {玩家: %s, QQ: %d}".formatted(bindCodeInfo.name(), qq));

        final StringBuilder s = new StringBuilder();
        s.append("添加绑定成功~\n");
        s.append("游戏名：%s".formatted(bindCodeInfo.name()));
        this.appendWaitingNamesForMsg(s, names);
        reply.add(s.toString());

        return reply;

    }

    @Override
    public void setGroupId(long id) {
        this.groupId = id;
    }

    @Override
    public long getGroupId() {
        return this.groupId;
    }

    void appendInfo(@NotNull TextComponent.Builder text, @NotNull QqBindApi.BindInfo info) {

        text.append(Component.text("==== QQ绑定信息 ====").color(NamedTextColor.GREEN));

        // 名字
        text.append(Component.newline());
        text.append(Component.text("玩家名: ").color(NamedTextColor.GREEN));
        text.append(Component.text(info.name()).color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(info.name())));


        // UUID
        final String uuid = info.uuid().toString();
        text.append(Component.newline());
        text.append(Component.text("UUID: ").color(NamedTextColor.GREEN));
        text.append(Component.text(uuid).color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(uuid)));


        // QQ
        final String qq = "%d".formatted(info.qq());
        text.append(Component.newline());
        text.append(Component.text("QQ: ").color(NamedTextColor.GREEN));
        text.append(Component.text(qq).color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(qq)));

        // 备注
        text.appendNewline();
        text.append(Component.text("备注：").color(NamedTextColor.GREEN));
        text.append(Component.text(info.remark()).color(NamedTextColor.GREEN));

        // 时间
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日_HH:mm:ss");
        final String formatted = simpleDateFormat.format(info.time());
        text.appendNewline();
        text.append(Component.text("时间：").color(NamedTextColor.GREEN));
        text.append(Component.text(formatted).color(NamedTextColor.GREEN));

    }

}
