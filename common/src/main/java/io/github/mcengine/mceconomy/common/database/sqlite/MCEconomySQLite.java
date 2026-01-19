package io.github.mcengine.mceconomy.common.database.sqlite;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.sql.*;

/**
 * SQLite implementation for MCEconomy.
 */
public class MCEconomySQLite implements IMCEconomyDB {
    /**
     * The active SQL connection instance.
     */
    private Connection conn;

    /**
     * Constructs a new SQLite database handler.
     * Creates the plugin data folder and database file if they do not exist.
     *
     * @param plugin The Bukkit/Spigot plugin instance.
     */
    public MCEconomySQLite(Plugin plugin) {
        File dataFolder = new File(plugin.getDataFolder(), "mceconomy.db");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the mceconomy table if it does not already exist in the SQLite file.
     *
     * @throws SQLException If an error occurs during table creation.
     */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mceconomy (" +
                     "player_uuid TEXT PRIMARY KEY NOT NULL, " +
                     "coin INTEGER NOT NULL DEFAULT 0, " +
                     "copper INTEGER NOT NULL DEFAULT 0, " +
                     "silver INTEGER NOT NULL DEFAULT 0, " +
                     "gold INTEGER NOT NULL DEFAULT 0)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Validates if the provided coin type is a valid column name.
     * Use this to prevent SQL Injection attacks via column name manipulation.
     *
     * @param coinType The coin type string to check.
     * @return true if valid, false otherwise.
     */
    private boolean isValidCoinType(String coinType) {
        if (coinType == null) return false;
        return coinType.equalsIgnoreCase("coin") ||
               coinType.equalsIgnoreCase("copper") ||
               coinType.equalsIgnoreCase("silver") ||
               coinType.equalsIgnoreCase("gold");
    }

    /**
     * Inserts a player into the database if they do not already exist.
     * Uses INSERT OR IGNORE for SQLite compatibility.
     *
     * @param playerUuid The UUID of the player to verify.
     * @return true if the operation executed successfully, false if a SQL error occurred.
     */
    @Override
    public boolean ensurePlayerExist(String playerUuid) {
        String sql = "INSERT OR IGNORE INTO mceconomy (player_uuid) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the balance of a specific coin type for a player.
     *
     * @param playerUuid The UUID of the player.
     * @param coinType   The column name (coin, copper, silver, gold).
     * @return The amount found in the database, or 0 if an error occurs.
     */
    @Override
    public int getCoin(String playerUuid, String coinType) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);

        ensurePlayerExist(playerUuid);
        String sql = "SELECT " + coinType + " FROM mceconomy WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Sets the balance of a specific coin type for a player to a specific amount.
     *
     * @param playerUuid The UUID of the player.
     * @param coinType   The column name.
     * @param amount     The new value to set.
     * @return true if the update was successful, false on error.
     */
    @Override
    public boolean setCoin(String playerUuid, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);

        ensurePlayerExist(playerUuid);
        String sql = "UPDATE mceconomy SET " + coinType + " = ? WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, playerUuid);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds an amount to the player's current balance for a specific coin type.
     *
     * @param playerUuid The UUID of the player.
     * @param coinType   The column name.
     * @param amount     The amount to add.
     * @return true if successful, false on error.
     */
    @Override
    public boolean addCoin(String playerUuid, String coinType, int amount) {
        // Validation is handled in getCoin/setCoin, but good practice to fail fast.
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);
        return setCoin(playerUuid, coinType, getCoin(playerUuid, coinType) + amount);
    }

    /**
     * Subtracts an amount from the player's current balance for a specific coin type.
     * Prevents the balance from dropping below 0.
     *
     * @param playerUuid The UUID of the player.
     * @param coinType   The column name.
     * @param amount     The amount to subtract.
     * @return true if transaction succeeded, false if insufficient funds or error.
     */
    @Override
    public boolean minusCoin(String playerUuid, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);
        int currentBalance = getCoin(playerUuid, coinType);
        if (currentBalance >= amount) {
            return setCoin(playerUuid, coinType, currentBalance - amount);
        }
        return false;
    }

    /**
     * Transfers an amount of a specific coin type from one player to another.
     * Verifies that the sender has enough balance before proceeding.
     *
     * @param senderUuid   The UUID of the player sending the coins.
     * @param receiverUuid The UUID of the player receiving the coins.
     * @param coinType     The column name.
     * @param amount       The amount to transfer.
     * @return true if transfer succeeded, false if sender has insufficient funds.
     */
    @Override
    public boolean sendCoin(String senderUuid, String receiverUuid, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);
        int senderBalance = getCoin(senderUuid, coinType);
        if (senderBalance >= amount) {
            boolean removed = setCoin(senderUuid, coinType, senderBalance - amount);
            if (removed) {
                return addCoin(receiverUuid, coinType, amount);
            }
        }
        return false;
    }

    /**
     * Closes the SQLite connection and releases the file lock.
     */
    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}
