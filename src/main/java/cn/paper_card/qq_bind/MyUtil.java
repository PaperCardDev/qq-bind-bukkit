package cn.paper_card.qq_bind;

import cn.paper_card.qq_bind.api.BindInfo;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

class MyUtil {

    static @NotNull BindInfo parseBindInfoJson(@NotNull JsonObject data) {
        final long qq = data.get("qq").getAsLong();
        final String uuid = data.get("uuid").getAsString();
        final long time = data.get("time").getAsLong();
        final String remark = data.get("remark").getAsString();
        return new BindInfo(uuid, qq, remark, time);
    }

    static @NotNull TextComponent copyable(@NotNull String text) {
        return Component.text(text)
                .clickEvent(ClickEvent.copyToClipboard(text))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制")))
                .decorate(TextDecoration.UNDERLINED);
    }

    static void appendBindInfo(@NotNull TextComponent.Builder text, @NotNull BindInfo info) {
        text.appendNewline();
        text.append(Component.text("QQ: "));
        final String qq = "%d".formatted(info.qq());
        text.append(copyable(qq));

        text.appendNewline();
        text.append(Component.text("UUID: "));
        text.append(copyable(info.uuid()));

        text.appendNewline();
        text.append(Component.text("时间: "));
        text.append(copyable("%d".formatted(info.time())));

        text.appendNewline();
        text.append(Component.text("备注: "));
        text.append(copyable(info.remark()));
    }
}
