package cn.paper_card.player_qq_bind;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.List;
import java.util.UUID;

public interface QqBindApi {
    record BindInfo(
            UUID uuid, // 玩家UUID

            String name, // 玩家名
            long qq, // 玩家QQ

            String remark, // 备注

            long time
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
        class DuplicatedCode extends Exception {
        }

        // 生成一个绑定验证码，返回生成的绑定验证码，如果生成的验证码重复，将返回0
        // 如果是同一个玩家，后生成的验证码会使前面的验证码失效
        int createCode(@NotNull UUID uuid, @NotNull String name) throws Exception;

        // 根据验证码取出玩家信息，如果验证码过期或不存在，返回null。
        @Nullable BindCodeInfo takeByCode(int code) throws Exception;

        // 查询所有的有验证码的玩家名
        @NotNull List<String> queryPlayerNames() throws Exception;

        int cleanOutdated() throws Exception;

        long getMaxAliveTime(); // 最大存活时间

        int getCodeCount();

    }

    interface QqBot {
        void sendAtMessage(long qq, @NotNull String message);
    }

    interface AutoQqBind {
        // 实现要求：仅仅是获取玩家可能的QQ号码
        long findPossibleQq(@NotNull UUID uuid, @NotNull String name) throws Exception;
    }

    interface BindApi {
        abstract class AreadyBindException extends Exception {
            abstract @NotNull BindInfo getBindInfo();
        }

        abstract class QqHasBeenBindedException extends Exception {
            abstract @NotNull BindInfo getBindInfo();
        }

        void addBind(@NotNull BindInfo info) throws Exception;

        boolean deleteByUuidAndQq(@NotNull UUID uuid, long qq) throws Exception;

        @Nullable BindInfo queryByUuid(@NotNull UUID uuid) throws Exception;

        @Nullable BindInfo queryByQq(long qq) throws Exception;

        void clearCache();
    }

    interface PreLoginRequest {
        @NotNull String getName();

        @NotNull UUID getUuid();

        @Nullable QqBot getQqBot();

        @Nullable AutoQqBind getAutoQqBind();
    }

    interface PreLoginResponse {
        @NotNull AsyncPlayerPreLoginEvent.Result result();

        @Nullable TextComponent kickMessage();

        long qq();

        int bindCode();
    }

    @NotNull BindApi getBindApi();

    @SuppressWarnings("unused")
    @NotNull BindCodeApi getBindCodeApi();

    @SuppressWarnings("unused")
    @NotNull PreLoginResponse handlePreLogin(@NotNull PreLoginRequest request);

    @SuppressWarnings("unused")
    @Nullable List<String> onMainGroupMessage(long qq, @NotNull String message);
}
