package cn.paper_card.qq_bind;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.qq_bind.api.QqBindApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;


public final class ThePlugin extends JavaPlugin implements Listener {

    private QqBindApiImpl qqBindApi = null;

    private final @NotNull TextComponent prefix;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull ConfigManagerImpl configManager;

    public ThePlugin() {

        this.configManager = new ConfigManagerImpl(this);


        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("QQ绑定").color(NamedTextColor.GOLD))
                .append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE))
                .build();

        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }


    @Override
    public void onLoad() {
        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);

        if (api == null) {
            throw new RuntimeException("无法连接到" + DatabaseApi.class.getSimpleName());
        }

        this.qqBindApi = new QqBindApiImpl(
                api.getRemoteMySQL().getConnectionImportant(),
                api.getRemoteMySQL().getConnectionUnimportant(),
                this.getSLF4JLogger(),
                this.configManager);

        this.getSLF4JLogger().info("注册%s...".formatted(QqBindApi.class.getSimpleName()));

        this.getServer().getServicesManager().register(QqBindApi.class, this.qqBindApi, this, ServicePriority.Highest);
    }

    @Override
    public void onEnable() {
        new TheCommand(this);
        this.configManager.onEnable();
    }

    @Override
    public void onDisable() {
        this.getServer().getServicesManager().unregisterAll(this);
        if (this.qqBindApi != null) this.qqBindApi.close();
        this.taskScheduler.cancelTasks(this);
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }

    @NotNull ConfigManagerImpl getConfigManager() {
        return this.configManager;
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build());
    }

    void sendException(@NotNull CommandSender sender, @NotNull Exception e) {

        final TextComponent.Builder text = Component.text();
        text.append(this.prefix);
        text.appendSpace();
        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(e.toString()).color(NamedTextColor.RED));
        }

        sender.sendMessage(text.build());
    }

    void sendWarning(@NotNull CommandSender sender, @NotNull String warning) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(warning).color(NamedTextColor.YELLOW))
                .build());
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull TextComponent info) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(info)
                .build());
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull QqBindApiImpl getQqBindApi() {
        return this.qqBindApi;
    }
}
