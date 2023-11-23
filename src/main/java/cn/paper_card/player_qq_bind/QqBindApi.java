package cn.paper_card.player_qq_bind;

import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface QqBindApi {
    record BindInfo(
            UUID uuid, // 玩家UUID
            long qq // 玩家QQ
    ) {
    }

    record BindCodeInfo(
            int code, // 验证码
            String name, // 玩家名
            UUID uuid, // 玩家UUID
            long time // 验证码创建时间
    ) {
    }

    interface BindCodeApi {
        // 生成一个绑定验证码，返回生成的绑定验证码，如果生成的验证码重复，将返回0
        // 如果是同一个玩家，后生成的验证码会使前面的验证码失效
        int createCode(@NotNull UUID uuid, @NotNull String name) throws Exception;

        // 根据验证码取出玩家信息，如果验证码过期或不存在，返回null。
        @Nullable BindCodeInfo takeByCode(int code) throws Exception;

        // 查询所有的有验证码的玩家名
        @NotNull List<String> queryPlayerNames() throws Exception;

        int cleanOutdated() throws Exception;

        int getPlayerCount();

        long getMaxAliveTime(); // 最大存活时间

    }

    interface QqBot {
        void sendAtMessage(long qq, @NotNull String message);
    }

    interface AutoQqBind {
        // 实现要求：仅仅是获取玩家可能的QQ号码
        long tryBind(@NotNull UUID uuid, @NotNull String name);
    }

    @SuppressWarnings("unused")
    @NotNull BindCodeApi getBindCodeApi();

    // 添加或更新玩家的QQ号码，添加返回true，更新返回false。
    @SuppressWarnings("unused")
    boolean addOrUpdateByUuid(@NotNull UUID uuid, long qq) throws Exception;

    // 根据UUID进行查询
    @SuppressWarnings("unused")
    @Nullable BindInfo queryByUuid(@NotNull UUID uuid) throws Exception;

    // 根据QQ查询绑定信息
    @SuppressWarnings("unused")
    @Nullable BindInfo queryByQq(long qq) throws Exception;

    @SuppressWarnings("unused")
    long onPreLoginCheck(@NotNull AsyncPlayerPreLoginEvent event, @Nullable QqBot qqBot, @Nullable AutoQqBind autoQqBind);

    @SuppressWarnings("unused")
    @Nullable List<String> onMainGroupMessage(long qq, @NotNull String message);
}
