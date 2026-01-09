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
     * @return true if the update was successful, false otherwise.
     */
    boolean setCoin(String playerUuid, String coinType, int amount);

    /**
     * Increases a player's balance.
     * @param playerUuid The UUID of the player.
     * @param coinType The column name.
     * @param amount The amount to add.
     * @return true if the addition was successful, false otherwise.
     */
    boolean addCoin(String playerUuid, String coinType, int amount);

    /**
     * Decreases a player's balance. 
     * Implementations should check for sufficient funds.
     * @param playerUuid The UUID of the player.
     * @param coinType The column name.
     * @param amount The amount to subtract.
     * @return true if the subtraction was successful, false if insufficient funds or error.
     */
    boolean minusCoin(String playerUuid, String coinType, int amount);

    /**
     * Transfers coins from one player to another.
     * @param senderUuid The UUID of the sender.
     * @param receiverUuid The UUID of the receiver.
     * @param coinType The column name.
     * @param amount The amount to transfer.
     * @return true if the transfer was completed successfully, false otherwise.
     */
    boolean sendCoin(String senderUuid, String receiverUuid, String coinType, int amount);

    /**
     * Checks if a player exists in the database; if not, creates a record with default values.
     * @param playerUuid The UUID of the player.
     * @return true if the player exists or was successfully created, false if a database error occurred.
     */
    boolean ensurePlayerExist(String playerUuid);

    /**
     * Closes the database connection safely.
     */
    void close();
}
