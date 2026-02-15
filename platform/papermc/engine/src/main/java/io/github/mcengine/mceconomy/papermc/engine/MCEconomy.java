package io.github.mcengine.mceconomy.papermc.engine;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mcextension.common.MCExtensionManager;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import io.github.mcengine.mceconomy.common.command.util.*;
import io.github.mcengine.mceconomy.common.database.mysql.MCEconomyMySQL;
import io.github.mcengine.mceconomy.common.database.sqlite.MCEconomySQLite;
import io.github.mcengine.mceconomy.common.listener.MCEconomyListenerManager;
import io.github.mcengine.mceconomy.common.listener.util.*;
import io.github.mcengine.mceconomy.common.tabcompleter.MCEconomyTabCompleter;
import io.github.mcengine.mcutil.MCUtil;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Main plugin class for MCEconomy.
 * Handles the lifecycle and registration of the economy system.
 */
public class MCEconomy extends JavaPlugin {

    /**
     * The provider handling database operations, coin logic, and managers.
     */
    private MCEconomyProvider provider;

    /**
     * The manager handling subcommand registration and execution.
     */
    private MCEconomyCommandManager commandManager;

    /**
     * The manager handling event listener registration.
     */
    private MCEconomyListenerManager listenerManager;

    /**
     * The manager handling loading and lifecycle of MCExtensions.
     */
    private MCExtensionManager extensionManager;

    /**
     * The executor used for running extension-related tasks.
     */
    private Executor executor;

    /**
     * Called when the plugin is enabled.
     * Initializes configuration, core components, services, and registers handlers.
     */
    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        saveDefaultConfig();

        // 2. Initialize Core Components
        IMCEconomyDB db = setupDatabase();
        this.executor = setupExecutor();

        // Managers must be initialized before the provider now
        this.commandManager = new MCEconomyCommandManager();
        this.listenerManager = new MCEconomyListenerManager(this);
        
        // Initialize Extension Manager
        this.extensionManager = new MCExtensionManager();

        // Inject everything into the Provider
        this.provider = new MCEconomyProvider(db, this.executor, commandManager, listenerManager);

        // 3. Register Managers as Bukkit Services
        Bukkit.getServicesManager().register(MCEconomyProvider.class, provider, this, ServicePriority.Normal);
        // We can still register these separately if other plugins look for them directly, 
        // or rely solely on MCEconomyProvider. For safety, we keep them registered.
        Bukkit.getServicesManager().register(MCEconomyCommandManager.class, commandManager, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(MCEconomyListenerManager.class, listenerManager, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(MCExtensionManager.class, extensionManager, this, ServicePriority.Normal);

        // 4. Setup Commands and Listeners
        registerSubcommands();
        PluginCommand economyCommand = getCommand("economy");
        if (economyCommand != null) {
            economyCommand.setExecutor(commandManager);
            economyCommand.setTabCompleter(new MCEconomyTabCompleter(commandManager));
        }
        registerListeners();

        // 5. Load Extensions
        extensionManager.loadAllExtensions(this, this.executor);

        // 6. Check for updates against remote git provider
        if (isNewVersionAvailable()) {
            getLogger().warning("A newer version of MCEconomy is available. Please update to the latest release.");
        }

        getLogger().info("MCEconomy Engine has been enabled!");
    }

    /**
     * Helper to determine the correct database implementation.
     */
    private IMCEconomyDB setupDatabase() {
        String dbType = getConfig().getString("db.type", "sqlite").toLowerCase();
        if ("mysql".equals(dbType)) {
            return new MCEconomyMySQL(this);
        }
        return new MCEconomySQLite(this);
    }

    /**
     * Helper to determine the correct Executor for the platform.
     */
    private Executor setupExecutor() {
        try {
            // Check if Folia's AsyncScheduler is available (Folia/Paper 1.20+)
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return task -> Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
        } catch (ClassNotFoundException e) {
            // Fallback to standard Bukkit Async Scheduler (Spigot/Legacy Paper)
            return task -> Bukkit.getScheduler().runTaskAsynchronously(this, task);
        }
    }

    /**
     * Called when the plugin is disabled.
     * Ensures database connections and extensions are closed properly.
     */
    @Override
    public void onDisable() {
        // Shutdown extensions first to allow them to use the database before it closes
        if (extensionManager != null) {
            extensionManager.disableAllExtensions(this, this.executor);
        }

        // Shutdown database connections
        if (provider != null) {
            provider.shutdown();
        }
        getLogger().info("MCEconomy Engine has been disabled!");
    }

    /**
     * Registers all subcommand handlers to the command manager.
     */
    private void registerSubcommands() {
        commandManager.register("help", new HandleHelp(commandManager));
        commandManager.register("get", new HandleGet(this, provider));
        commandManager.register("add", new HandleAdd(this, provider));
        commandManager.register("minus", new HandleMinus(this, provider));
        commandManager.register("set", new HandleSet(this, provider));
        commandManager.register("send", new HandleSend(this, provider));
        commandManager.register("convert", new HandleConvert(this, provider));
    }

    /**
     * Registers all event listeners to the listener manager.
     */
    private void registerListeners() {
        listenerManager.register(new HandleEnsurePlayerExist(provider));
        listenerManager.register(new HandleCoinItem(this, provider));
    }

    /**
     * Checks whether a newer version exists on the configured git provider.
     */
    private boolean isNewVersionAvailable() {
        PluginMeta pluginMeta = getPluginMeta();
        String currentVersion = pluginMeta != null ? pluginMeta.getVersion() : "unspecified";
        String gitType = getConfig().getString("git.type", "github");
        String org = getConfig().getString("git.org", "MCEngine");
        String repo = getConfig().getString("git.repo", "mceconomy");
        String token = resolveGitToken(gitType);

        try {
            return MCUtil.compareVersion(gitType, currentVersion, org, repo, token);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Unsupported git provider configured: " + gitType);
        } catch (IOException e) {
            getLogger().warning("Failed to check remote version: " + e.getMessage());
        }
        return false;
    }

    private String resolveGitToken(String gitType) {
        String envOverride = null;
        if ("github".equalsIgnoreCase(gitType)) {
            envOverride = System.getenv("USER_GITHUB_TOKEN");
            if (envOverride == null || envOverride.isEmpty()) {
                envOverride = System.getenv("GITHUB_TOKEN");
            }
            String configured = getConfig().getString("git.github.token");
            if (configured != null && !configured.isEmpty()) {
                return configured;
            }
        } else if ("gitlab".equalsIgnoreCase(gitType)) {
            envOverride = System.getenv("USER_GITLAB_TOKEN");
            if (envOverride == null || envOverride.isEmpty()) {
                envOverride = System.getenv("GITLAB_TOKEN");
            }
            String configured = getConfig().getString("git.gitlab.token");
            if (configured != null && !configured.isEmpty()) {
                return configured;
            }
        }

        if (envOverride != null && !envOverride.isEmpty()) {
            return envOverride;
        }

        String fallback = getConfig().getString("git.token");
        return fallback != null ? fallback : "";
    }

}
