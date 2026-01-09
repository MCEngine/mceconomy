package io.github.mcengine.mceconomy.api.database;

/**
 * Interface for MCEconomy database operations.
 * Supports multiple coin types (coin, copper, silver, gold).
 */
public interface IMCEconomyDB {

    /**
     * Retrieves the balance for a specific coin type.
     * @param playerUuid The UUID of the player.
     * @param coinType The column name (coin, copper, silver, gold).
     * @return The amount stored in the database.
     */
    int getCoin(String playerUuid, String coinType);

    /**
     * Sets a player's balance to a specific amount.
     * @param playerUuid The UUID of the player.
     * @param coinType The column name.
     * @param amount The new balance.
     */
    void setCoin(String playerUuid, String coinType, int amount);

    /**
     * Increases a player's balance.
     * @param playerUuid The UUID of the player.
     * @param coinType The column name.
     * @param amount The amount to add.
     */
    void addCoin(String playerUuid, String coinType, int amount);

    /**
     * Decreases a player's balance.
     * @param playerUuid The UUID of the player.
     * @param coinType The column name.
     * @param amount The amount to subtract.
     */
    void minusCoin(String playerUuid, String coinType, int amount);

    /**
     * Transfers coins from one player to another.
     * @param senderUuid The UUID of the sender.
     * @param receiverUuid The UUID of the receiver.
     * @param coinType The column name.
     * @param amount The amount to transfer.
     */
    void sendCoin(String senderUuid, String receiverUuid, String coinType, int amount);

    /**
     * Checks if a player exists in the database; if not, creates a record with default values (1).
     * @param playerUuid The UUID of the player.
     */
    void ensurePlayerExist(String playerUuid);

    /**
     * Closes the database connection safely.
     */
    void close();
}