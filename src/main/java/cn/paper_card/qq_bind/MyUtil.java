package cn.paper_card.qq_bind;

import cn.paper_card.qq_bind.api.BindInfo;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

class MyUtil {

    static @NotNull BindInfo parseBindInfoJson(@NotNull JsonObject data) {
        final long qq = data.get("qq").getAsLong();
        final String uuid = data.get("uuid").getAsString();
        final long time = data.get("time").getAsLong();
        final String remark = data.get("remark").getAsString();
        return new BindInfo(uuid, qq, remark, time);
    }

    static void appendBindInfo(@NotNull TextComponent.Builder text, @NotNull BindInfo info) {
        text.appendNewline();
        text.append(Component.text("QQ: "));
        text.append(Component.text(info.qq()));

        text.appendNewline();
        text.append(Component.text("UUID: "));
        text.append(Component.text(info.uuid()));

        text.appendNewline();
        text.append(Component.text("时间: "));
        text.append(Component.text(info.time()));

        text.appendNewline();
        text.append(Component.text("备注: "));
        text.append(Component.text(info.remark()));
    }

    static @NotNull String toReadableTime(long ms) {
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
}
