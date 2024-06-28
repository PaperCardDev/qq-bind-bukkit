package cn.paper_card.qq_bind;


import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.qq_bind.api.QqBindApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;


public final class ThePlugin extends JavaPlugin implements Listener {
    private final @NotNull TaskScheduler taskScheduler;

    private PaperClientApi paperClientApi = null;

    private final @NotNull QqBindApiImpl qqBindApi;


    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.qqBindApi = new QqBindApiImpl(this);
    }

    void appendPrefix(@NotNull TextComponent.Builder text) {
        text.append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE));
        text.append(Component.text("QQ绑定").color(NamedTextColor.GOLD));
        text.append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE));
    }


    void loadApi() {
        if (this.paperClientApi != null) return;
        this.paperClientApi = this.getServer().getServicesManager().load(PaperClientApi.class);
        if (this.paperClientApi == null) {
            throw new RuntimeException("Fail to load PaperClientApi");
        }
    }


    @Override
    public void onLoad() {
        this.loadApi();
        this.getServer().getServicesManager().register(QqBindApi.class, this.qqBindApi, this, ServicePriority.Highest);
    }

    @Override
    public void onEnable() {
        this.loadApi();
        new MainCommand(this).register(this);
    }

    @Override
    public void onDisable() {
        this.paperClientApi = null;
        this.taskScheduler.cancelTasks(this);
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull PaperClientApi getPaperClientApi() throws Exception {
        final PaperClientApi api = this.paperClientApi;
        if (api == null) throw new Exception("PaperClientApi is null!");
        return api;
    }

    @NotNull QqBindApiImpl getQqBindApi() {
        return this.qqBindApi;
    }
}
