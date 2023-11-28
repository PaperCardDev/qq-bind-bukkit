package cn.paper_card.qq_bind;

import org.jetbrains.annotations.NotNull;

class MyUtil {
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
