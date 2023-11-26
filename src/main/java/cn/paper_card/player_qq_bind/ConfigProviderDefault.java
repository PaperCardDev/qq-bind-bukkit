package cn.paper_card.player_qq_bind;

import org.jetbrains.annotations.NotNull;

public class ConfigProviderDefault implements ConfigManager {
    @Override
    public @NotNull String getMinedownBindInfo() {
        return """
                &green&==== QQ绑定信息 ====
                游戏名：[%name%](format=underlined copy_to_clipboard=%name% hover_text=点击复制)
                UUID：[%uuid%](format=underlined copy_to_clipboard=%uuid% hover_text=点击复制)
                QQ：[%qq%](format=underlined copy_to_clipboard=%qq% hover_text=点击复制)
                备注：%remark%
                时间：%datetime%""";
    }

    @Override
    public @NotNull String getMinedownKickMessageBindCode() {
        return """
                &aqua&[ QQ绑定 ]
                &green&QQ绑定验证码：[%code%](format=underlined,bold color=light_purple) &yellow&（有效时间：%validTime%）
                &dark_green&请在我们的QQ群 [%group%](format=underlined,bold color=gold) 里直接发送该数字
                &yellow&如果QQ机器人在线（当前%botState%），会自动处理
                &gray&游戏角色：%name% (%uuid%)""";
    }

    @Override
    public @NotNull String getMinedownKickMessageAutoBind() {
        return """
                [ QQ绑定 | 自动绑定 ]
                已为你自动添加了QQ绑定
                游戏角色：%name% (%uuid%)
                QQ：%qq%
                请确认QQ号码无误，如果错误，请联系管理员
                此为通知消息，请重新连接服务器""";
    }

    @Override
    public @NotNull String getGroupMessageInvalidCode() {
        return """
                [ QQ绑定 ]
                不存在或已过期失效的QQ绑定验证码：%code%
                请重新获取新验证码~""";
    }

    @Override
    public @NotNull String getGroupMessageAlreadyBind() {
        return """
                [ QQ绑定 ]
                你已经绑定了一个游戏角色
                不能再绑定另外一个角色
                你的游戏名：%name%
                如需改绑，请联系管理员""";
    }

    @Override
    public @NotNull String getGroupMessageBindOk() {
        return """
                [ QQ绑定 ]
                添加绑定成功 :D
                你的游戏名：%name%""";
    }

    @Override
    public @NotNull String getGroupMessageAutoBind() {
        return """
                [ QQ绑定 ]
                为你自动添加了QQ绑定
                游戏名：%name%
                如果这不是你，请联系管理员""";
    }

    @Override
    public @NotNull String getRemarkForBindCode() {
        return "验证码绑定";
    }

    @Override
    public @NotNull String getRemarkForAutoBind() {
        return "自动绑定";
    }

    @Override
    public @NotNull String getRemarkForByAdd() {
        return "add指令，%operator%执行";
    }
}
