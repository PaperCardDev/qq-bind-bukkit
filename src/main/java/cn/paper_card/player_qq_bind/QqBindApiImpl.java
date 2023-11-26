package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private final @NotNull ConfigManager configManager;

    private long groupId = 0;

    QqBindApiImpl(@NotNull DatabaseApi.MySqlConnection connection1, @NotNull DatabaseApi.MySqlConnection connection2, @NotNull Logger logger, @NotNull ConfigManager configManager) {
        this.bindApi = new BindApiImpl(connection1);
        this.bindCodeApi = new BindCodeApiImpl(connection2);
        this.logger = logger;
        this.configManager = configManager;
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

    private @NotNull Component kickMessageAutoBind(@NotNull PreLoginRequest request, long qq) {

        final String string = this.configManager.getMinedownKickMessageAutoBind();

        final MineDown mineDown = new MineDown(string);
        mineDown.replace("name", request.getName(),
                "uuid", request.getUuid().toString(),
                "qq", "%d".formatted(qq));

        return mineDown.toComponent();
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
                    this.configManager.getRemarkForAutoBind(), System.currentTimeMillis()));

        } catch (Exception e) {
            return this.kickWhenException(e);
        }

        getLogger().info("自动添加了QQ绑定：{name: %s, qq: %d}".formatted(request.getName(), qq));

        // 在群里发送消息通知
        final QqBot qqBot = request.getQqBot();

        if (qqBot != null) {
            String msg = this.configManager.getGroupMessageAutoBind();
            msg = msg.replace("%name%", request.getName());
            qqBot.sendAtMessage(qq, msg);
        }

        return new PreLoginResponseImpl(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMessageAutoBind(request, qq), 0);
    }

    private Component kickMessageBindCode(int code, @NotNull PreLoginRequest request) {

        final long gid = this.getGroupId();

        final String string = this.configManager.getMinedownKickMessageBindCode();
        final MineDown mineDown = new MineDown(string);
        mineDown.replace("code", "%d".formatted(code),
                "validTime", this.toReadableTime(this.getBindCodeApi().getMaxAliveTime()),
                "group", "%d".formatted(gid),
                "botState", request.getQqBot() != null ? "在线" : "不在线",
                "name", request.getName(),
                "uuid", request.getUuid().toString()
        );
        return mineDown.toComponent();
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

        return new PreLoginResponseImpl(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, this.kickMessageBindCode(code, request), 0, code);
    }


    // 确保列表非空
    private void appendNames(@NotNull StringBuilder stringBuilder, @NotNull List<String> names, int size) {
        stringBuilder.append(names.get(0));
        for (int i = 1; i < size; ++i) {
            stringBuilder.append('、');
            stringBuilder.append(names.get(i));
        }
    }

    private void appendWaitingPlayers(@NotNull List<String> names, @NotNull List<String> reply) {
        final int size = names.size();
        if (size == 0) return;

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("继续等待玩家验证：");
        this.appendNames(stringBuilder, names, size);
        reply.add(stringBuilder.toString());
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
            String msg = this.configManager.getGroupMessageInvalidCode();
            msg = msg.replace("%code%", "%d".formatted(code));

            reply.add(msg);
            this.appendWaitingPlayers(names, reply);

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

            // 已经绑定
            if (bindInfo != null) {
                String msg = this.configManager.getGroupMessageAlreadyBind();
                msg = msg.replace("%name%", bindInfo.name());
                reply.add(msg);

                this.appendWaitingPlayers(names, reply);
                return reply;
            }
        }


        // 添加绑定

        try {
            this.getBindApi().addBind(new BindInfo(bindCodeInfo.uuid(), bindCodeInfo.name(), qq, this.configManager.getRemarkForBindCode(), System.currentTimeMillis()));
        } catch (Exception e) {
            this.handleException(e);
            reply.add(e.toString());
            return reply;
        }

        this.getLogger().info("添加QQ绑定 {玩家: %s, QQ: %d}".formatted(bindCodeInfo.name(), qq));

        String msg = this.configManager.getGroupMessageBindOk();
        msg = msg.replace("%name%", bindCodeInfo.name());

        reply.add(msg);
        this.appendWaitingPlayers(names, reply);

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

        final String minedownBindInfo = this.configManager.getMinedownBindInfo();
        final MineDown mineDown = new MineDown(minedownBindInfo);

        // 时间
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日_HH:mm:ss");
        final String formatted = simpleDateFormat.format(info.time());

        mineDown.replace(
                "name", info.name(),
                "uuid", info.uuid().toString(),
                "qq", "%d".formatted(info.qq()),
                "remark", info.remark(),
                "datetime", formatted
        );

        text.append(mineDown.toComponent());
    }

}
