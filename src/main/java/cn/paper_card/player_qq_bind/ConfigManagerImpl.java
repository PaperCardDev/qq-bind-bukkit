package cn.paper_card.player_qq_bind;

import org.jetbrains.annotations.NotNull;

class ConfigManagerImpl implements ConfigManager {

    private final @NotNull ThePlugin plugin;

    private final @NotNull ConfigProviderDefault aDefault;

    private final static String PATH_MINEDOWN_BIND_INFO = "minedown.bind-info";
    private final static String PATH_MINEDOWN_KICK_MESSAGE_BIND_CODE = "minedown.kick-message.bind-code";

    private final static String PATH_MINEDOWN_KICK_MESSAGE_AUTO_BIND = "minedown.kick-message.auto-bind";

    private final static String PATH_GROUP_MESSAGE_INVALID_CODE = "group-message.invalid-code";

    private final static String PATH_GROUP_MESSAGE_ALREADY_BIND = "group-message.already-bind";

    private final static String PATH_GROUP_MESSAGE_BIND_OK = "group-message.bind-ok";

    private final static String PATH_GROUP_MESSAGE_AUTO_BIND = "group-message.auto-bind";

    private final static String PATH_REMARK_BIND_CODE = "remark.bind-code";
    private final static String PATH_REMARK_AUTO_BIND = "remark.auto-bind";

    private final static String PATH_REMARK_BY_ADD = "remark.by-add";

    ConfigManagerImpl(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
        this.aDefault = new ConfigProviderDefault();
    }

    @Override
    public @NotNull String getMinedownBindInfo() {
        return this.plugin.getConfig().getString(PATH_MINEDOWN_BIND_INFO, this.aDefault.getMinedownBindInfo());
    }

    void setMinedownBindInfo(@NotNull String minedown) {
        this.plugin.getConfig().set(PATH_MINEDOWN_BIND_INFO, minedown);
    }

    @Override
    public @NotNull String getMinedownKickMessageBindCode() {
        return this.plugin.getConfig().getString(PATH_MINEDOWN_KICK_MESSAGE_BIND_CODE, this.aDefault.getMinedownKickMessageBindCode());
    }

    void setMinedownKickMessageBindCode(@NotNull String minedown) {
        this.plugin.getConfig().set(PATH_MINEDOWN_KICK_MESSAGE_BIND_CODE, minedown);
    }


    @Override
    public @NotNull String getMinedownKickMessageAutoBind() {
        return this.plugin.getConfig().getString(PATH_MINEDOWN_KICK_MESSAGE_AUTO_BIND, this.aDefault.getMinedownKickMessageAutoBind());
    }

    void setMinedownKickMessageAutoBind(@NotNull String minedown) {
        this.plugin.getConfig().set(PATH_MINEDOWN_KICK_MESSAGE_AUTO_BIND, minedown);
    }


    @Override
    public @NotNull String getGroupMessageInvalidCode() {
        return this.plugin.getConfig().getString(PATH_GROUP_MESSAGE_INVALID_CODE, this.aDefault.getGroupMessageInvalidCode());
    }

    void setGroupMessageInvalidCode(@NotNull String message) {
        this.plugin.getConfig().set(PATH_GROUP_MESSAGE_INVALID_CODE, message);
    }

    @Override
    public @NotNull String getGroupMessageAlreadyBind() {
        return this.plugin.getConfig().getString(PATH_GROUP_MESSAGE_ALREADY_BIND, this.aDefault.getGroupMessageAlreadyBind());
    }

    void setGroupMessageAlreadyBind(@NotNull String message) {
        this.plugin.getConfig().set(PATH_GROUP_MESSAGE_ALREADY_BIND, message);
    }

    @Override
    public @NotNull String getGroupMessageBindOk() {
        return this.plugin.getConfig().getString(PATH_GROUP_MESSAGE_BIND_OK, this.aDefault.getGroupMessageBindOk());
    }

    void setGroupMessageBindOk(@NotNull String message) {
        this.plugin.getConfig().set(PATH_GROUP_MESSAGE_BIND_OK, message);
    }

    @Override
    public @NotNull String getGroupMessageAutoBind() {
        return this.plugin.getConfig().getString(PATH_GROUP_MESSAGE_AUTO_BIND, this.aDefault.getGroupMessageAutoBind());
    }

    void setGroupMessageAutoBind(@NotNull String message) {
        this.plugin.getConfig().set(PATH_GROUP_MESSAGE_AUTO_BIND, message);
    }


    @Override
    public @NotNull String getRemarkForBindCode() {
        return this.plugin.getConfig().getString(PATH_REMARK_BIND_CODE, this.aDefault.getRemarkForBindCode());
    }

    void setRemarkForBindCode(@NotNull String remark) {
        this.plugin.getConfig().set(PATH_REMARK_BIND_CODE, remark);
    }

    @Override
    public @NotNull String getRemarkForAutoBind() {
        return this.plugin.getConfig().getString(PATH_REMARK_AUTO_BIND, this.aDefault.getRemarkForAutoBind());
    }


    void setRemarkForAutoBind(@NotNull String remark) {
        this.plugin.getConfig().set(PATH_REMARK_AUTO_BIND, remark);
    }

    @Override
    public @NotNull String getRemarkForByAdd() {
        return this.plugin.getConfig().getString(PATH_REMARK_BY_ADD, this.aDefault.getRemarkForByAdd());
    }

    void setRemarkForByAdd(@NotNull String remark) {
        this.plugin.getConfig().set(PATH_REMARK_BY_ADD, remark);
    }

    void onEnable() {
        this.setMinedownBindInfo(this.getMinedownBindInfo());
        this.setMinedownKickMessageAutoBind(this.getMinedownKickMessageAutoBind());
        this.setMinedownKickMessageBindCode(this.getMinedownKickMessageBindCode());

        this.setGroupMessageInvalidCode(this.getGroupMessageInvalidCode());
        this.setGroupMessageAlreadyBind(this.getGroupMessageAlreadyBind());
        this.setGroupMessageBindOk(this.getGroupMessageBindOk());
        this.setGroupMessageAutoBind(this.getGroupMessageAutoBind());

        this.setRemarkForByAdd(this.getRemarkForByAdd());
        this.setRemarkForAutoBind(this.getRemarkForAutoBind());
        this.setRemarkForBindCode(this.getRemarkForBindCode());

        this.plugin.saveConfig();
    }
}