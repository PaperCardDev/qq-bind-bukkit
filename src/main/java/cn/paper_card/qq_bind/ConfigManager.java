package cn.paper_card.qq_bind;

import org.jetbrains.annotations.NotNull;

interface ConfigManager {
    @NotNull String getMinedownBindInfo();

    @NotNull String getMinedownKickMessageBindCode();

    @NotNull String getMinedownKickMessageAutoBind();

    @NotNull String getGroupMessageInvalidCode();

    @NotNull String getGroupMessageAlreadyBind();

    @NotNull String getGroupMessageBindOk();

    @NotNull String getGroupMessageAutoBind();

    @NotNull String getRemarkForBindCode();

    @NotNull String getRemarkForAutoBind();

    @NotNull String getRemarkForByAdd();

}
