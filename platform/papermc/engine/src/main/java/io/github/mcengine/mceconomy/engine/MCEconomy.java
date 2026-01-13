package io.github.mcengine.mceconomy.engine;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import io.github.mcengine.mceconomy.common.command.util.*;
import io.github.mcengine.mceconomy.common.database.mysql.MCEconomyMySQL;
import io.github.mcengine.mceconomy.common.database.sqlite.MCEconomySQLite;
import io.github.mcengine.mceconomy.common.listener.MCEconomyListenerManager;
import io.github.mcengine.mceconomy.common.listener.util.HandleEnsurePlayerExist;
import io.github.mcengine.mceconomy.common.tabcompleter.MCEconomyTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;

/**
 * Main plugin class for MCEconomy.
 * Handles the lifecycle and registration of the economy system.
 */
public class MCEconomy extends JavaPlugin {

    /**
     * The provider handling database operations and coin logic.
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
     * Called when the plugin is enabled.
     * Initializes configuration, core components, services, and registers handlers.
     */
    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        saveDefaultConfig();

        // 2. Initialize Core Components
        // Initialize Database Implementation based on config
        IMCEconomyDB db = setupDatabase();
        
        // Initialize Platform-Specific Executor
        // This logic works for Spigot, Paper, and Folia
        Executor asyncExecutor = setupExecutor();

        this.provider = new MCEconomyProvider(db, asyncExecutor);
        this.commandManager = new MCEconomyCommandManager();
        this.listenerManager = new MCEconomyListenerManager(this);

        // 3. Register Managers as Bukkit Services
        Bukkit.getServicesManager().register(MCEconomyProvider.class, provider, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(MCEconomyCommandManager.class, commandManager, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(MCEconomyListenerManager.class, listenerManager, this, ServicePriority.Normal);

        // 4. Setup Commands and Listeners
        registerSubcommands();
        PluginCommand economyCommand = getCommand("economy");
        if (economyCommand != null) {
            economyCommand.setExecutor(commandManager);
            economyCommand.setTabCompleter(new MCEconomyTabCompleter(commandManager));
        }
        registerListeners();

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
     * Ensures database connections are closed properly.
     */
    @Override
    public void onDisable() {
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
    }

    /**
     * Registers all event listeners to the listener manager.
     */
    private void registerListeners() {
        listenerManager.register(new HandleEnsurePlayerExist(provider));
    }

    /**
     * Gets the active economy provider.
     * @return The MCEconomyProvider instance.
     */
    public MCEconomyProvider getProvider() {
        return this.provider;
    }

    /**
     * Gets the command manager for subcommands.
     * @return The MCEconomyCommandManager instance.
     */
    public MCEconomyCommandManager getCommandManager() {
        return this.commandManager;
    }

    /**
     * Gets the listener manager for internal events.
     * @return The MCEconomyListenerManager instance.
     */
    public MCEconomyListenerManager getListenerManager() {
        return this.listenerManager;
    }
}
