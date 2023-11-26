package cn.paper_card.player_qq_bind;

import net.kyori.adventure.text.Component;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


record PreLoginResponseImpl(@NotNull AsyncPlayerPreLoginEvent.Result result, @Nullable Component kickMessage,
                            long qq, int bindCode) implements QqBindApi.PreLoginResponse {

    PreLoginResponseImpl(AsyncPlayerPreLoginEvent.@NotNull Result result, @Nullable Component kickMessage, long qq) {
        this(result, kickMessage, qq, -1);
    }

    PreLoginResponseImpl(AsyncPlayerPreLoginEvent.@NotNull Result result, @Nullable Component kickMessage, long qq, int bindCode) {
        this.result = result;
        this.kickMessage = kickMessage;
        this.qq = qq;
        this.bindCode = bindCode;
    }
}
