package io.github.mcengine.mceconomy.common;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import io.github.mcengine.mceconomy.common.listener.MCEconomyListenerManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * High-level provider that acts as a wrapper for the underlying database.
 * <p>
 * This class is platform-agnostic. It does not depend on Bukkit/Spigot directly,
 * instead using an injected {@link Executor} to handle asynchronous operations
 * in a way compatible with Spigot, Paper, and Folia.
 * </p>
 */
public class MCEconomyProvider {

    /**
     * The singleton instance of the provider.
     */
    private static MCEconomyProvider instance;

    /**
     * The underlying database implementation (e.g., MySQL or SQLite).
     */
    private final IMCEconomyDB db;

    /**
     * The platform-specific executor used to run database tasks off the main thread.
     */
    private final Executor asyncExecutor;

    /**
     * The manager handling subcommand registration and execution.
     */
    private final MCEconomyCommandManager commandManager;

    /**
     * The manager handling event listener registration.
     */
    private final MCEconomyListenerManager listenerManager;

    /**
     * The default currency identifier used when no specific coin type is provided.
     */
    private static final String DEFAULT_COIN = "coin";

    /**
     * Initializes the provider with a database implementation and an async executor.
     * Sets the static singleton instance upon creation.
     *
     * @param db              The database logic implementation.
     * @param asyncExecutor   The executor (e.g., Bukkit scheduler or Folia async scheduler).
     * @param commandManager  The command manager instance.
     * @param listenerManager The listener manager instance.
     */
    public MCEconomyProvider(IMCEconomyDB db, Executor asyncExecutor, MCEconomyCommandManager commandManager, MCEconomyListenerManager listenerManager) {
        this.db = db;
        this.asyncExecutor = asyncExecutor;
        this.commandManager = commandManager;
        this.listenerManager = listenerManager;
        instance = this; // Set the singleton instance
    }

    /**
     * Gets the active singleton instance of the MCEconomyProvider.
     * Use this method to access the API without depending on the Engine class.
     *
     * @return The active MCEconomyProvider instance, or null if not initialized.
     */
    public static MCEconomyProvider getInstance() {
        return instance;
    }

    /**
     * Internal helper to wrap blocking database calls into a CompletableFuture.
     *
     * @param <T>      The return type.
     * @param supplier The database operation logic.
     * @return A CompletableFuture tracking the asynchronous task.
     */
    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
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

    // --- GETTERS ---

    /**
     * Gets the balance of the default 'coin' type asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account (e.g., "PLAYER", "CLAN").
     * @return A Future that completes with the current balance.
     */
    public CompletableFuture<Integer> getCoin(String accountUuid, String accountType) {
        return getCoin(accountUuid, accountType, DEFAULT_COIN);
    }

    /**
     * Gets the balance for a specific coin type asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The type of currency (e.g., "gold", "silver").
     * @return A Future that completes with the current balance.
     */
    public CompletableFuture<Integer> getCoin(String accountUuid, String accountType, String coinType) {
        return runAsync(() -> db.getCoin(accountUuid, accountType, coinType));
    }

    // --- SETTERS ---

    /**
     * Sets the balance of the default 'coin' type asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param amount      The new amount to set.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> setCoin(String accountUuid, String accountType, int amount) {
        return setCoin(accountUuid, accountType, DEFAULT_COIN, amount);
    }

    /**
     * Sets the balance for a specific coin type asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The type of currency.
     * @param amount      The new amount to set.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> setCoin(String accountUuid, String accountType, String coinType, int amount) {
        return runAsync(() -> db.setCoin(accountUuid, accountType, coinType, amount));
    }

    // --- ADD ---

    /**
     * Adds an amount to the default 'coin' balance asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param amount      Amount to add.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> addCoin(String accountUuid, String accountType, int amount) {
        return addCoin(accountUuid, accountType, DEFAULT_COIN, amount);
    }

    /**
     * Adds an amount to a specific coin type balance asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The type of currency.
     * @param amount      Amount to add.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> addCoin(String accountUuid, String accountType, String coinType, int amount) {
        return runAsync(() -> db.addCoin(accountUuid, accountType, coinType, amount));
    }

    // --- MINUS ---

    /**
     * Subtracts an amount from the default 'coin' balance asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param amount      Amount to subtract.
     * @return A Future that completes with true if successful, false if insufficient funds.
     */
    public CompletableFuture<Boolean> minusCoin(String accountUuid, String accountType, int amount) {
        return minusCoin(accountUuid, accountType, DEFAULT_COIN, amount);
    }

    /**
     * Subtracts an amount from a specific coin type balance asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The type of currency.
     * @param amount      Amount to subtract.
     * @return A Future that completes with true if successful, false if insufficient funds.
     */
    public CompletableFuture<Boolean> minusCoin(String accountUuid, String accountType, String coinType, int amount) {
        return runAsync(() -> db.minusCoin(accountUuid, accountType, coinType, amount));
    }

    // --- SEND ---

    /**
     * Sends default 'coin' currency from one account to another asynchronously.
     *
     * @param senderUuid   Sender account UUID.
     * @param senderType   Sender account type.
     * @param receiverUuid Receiver account UUID.
     * @param receiverType Receiver account type.
     * @param amount       Amount to transfer.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, int amount) {
        return sendCoin(senderUuid, senderType, receiverUuid, receiverType, DEFAULT_COIN, amount);
    }

    /**
     * Sends a specific coin type from one account to another asynchronously.
     *
     * @param senderUuid   Sender account UUID.
     * @param senderType   Sender account type.
     * @param receiverUuid Receiver account UUID.
     * @param receiverType Receiver account type.
     * @param coinType     The type of currency.
     * @param amount       Amount to transfer.
     * @return A Future that completes with true if successful, false if sender has insufficient funds.
     */
    public CompletableFuture<Boolean> sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, String coinType, int amount) {
        return runAsync(() -> db.sendCoin(senderUuid, senderType, receiverUuid, receiverType, coinType, amount));
    }

    // --- UTILITY ---

    /**
     * Ensures the account has an entry in the database asynchronously.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @return A Future that completes with true if the account exists or was successfully created.
     */
    public CompletableFuture<Boolean> ensureAccountExist(String accountUuid, String accountType) {
        return runAsync(() -> db.ensureAccountExist(accountUuid, accountType));
    }

    /**
     * Properly shuts down the database connection and releases resources.
     */
    public void shutdown() {
        if (db != null) {
            db.close();
        }
        instance = null; // Clear singleton
    }
}
