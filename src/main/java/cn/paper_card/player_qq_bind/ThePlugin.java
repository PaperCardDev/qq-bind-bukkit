package cn.paper_card.player_qq_bind;

import cn.paper_card.database.DatabaseApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;


public final class ThePlugin extends JavaPlugin implements Listener {

    private final @NotNull QqBindApiImpl qqBindApi;

    private final @NotNull TextComponent prefix;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull ConfigManagerImpl configManager;

    public ThePlugin() {

        @NotNull DatabaseApi databaseApi = this.getDatabaseApi();

        this.configManager = new ConfigManagerImpl(this);

        this.qqBindApi = new QqBindApiImpl(
                databaseApi.getRemoteMySqlDb().getConnectionImportant(),
                databaseApi.getRemoteMySqlDb().getConnectionUnimportant(),
                this.getLogger(),
                this.configManager);

        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("QQ绑定").color(NamedTextColor.GOLD))
                .append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE))
                .build();

        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    private @NotNull DatabaseApi getDatabaseApi() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("Database");
        if (!(plugin instanceof DatabaseApi api)) throw new NoSuchElementException("Database插件未安装！");
        return api;
    }

    @Override
    public void onLoad() {
        this.getServer().getServicesManager().register(QqBindApi.class, this.qqBindApi, this, ServicePriority.Highest);
    }

    @Override
    public void onEnable() {
        new TheCommand(this);

        final Plugin plugin = this.getServer().getPluginManager().getPlugin("OnPreLogin");
        if (plugin != null) {
            this.getLogger().info("OnPreLogin插件安装，不注册事件监听");
        } else {
            this.getServer().getPluginManager().registerEvents(this, this);
        }

        this.configManager.onEnable();
    }

    @Override
    public void onDisable() {
        this.getServer().getServicesManager().unregisterAll(this);
        this.qqBindApi.close();
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

    @EventHandler
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        final QqBindApi.PreLoginRequest request = this.getQqBindApi().createRequest(event);
        final QqBindApi.PreLoginResponse response = this.getQqBindApi().handlePreLogin(request);
        event.setLoginResult(response.result());
        final Component msg = response.kickMessage();
        if (msg != null) event.kickMessage(msg);
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull QqBindApiImpl getQqBindApi() {
        return this.qqBindApi;
    }
}
