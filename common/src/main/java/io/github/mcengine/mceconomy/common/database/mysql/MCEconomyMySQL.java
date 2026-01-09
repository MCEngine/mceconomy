package io.github.mcengine.mceconomy.common.database.mysql;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.sql.*;
import java.util.UUID;

/**
 * MySQL implementation for MCEconomy.
 */
public class MCEconomyMySQL implements IMCEconomyDB {
    /**
     * The active SQL connection instance.
     */
    private Connection conn;

    /**
     * Constructs a new MySQL database handler.
     * Initializes connection settings from the plugin configuration and attempts to connect.
     *
     * @param plugin The Bukkit/Spigot plugin instance.
     */
    public MCEconomyMySQL(Plugin plugin) {
        String dbUser = plugin.getConfig().getString("db.mysql.user");
        String dbPass = plugin.getConfig().getString("db.mysql.password");
        String dbHost = plugin.getConfig().getString("db.mysql.host");
        String dbName = plugin.getConfig().getString("db.mysql.database");
        String dbPort = plugin.getConfig().getString("db.mysql.port", "3306");
        String dbSsl = plugin.getConfig().getString("db.mysql.ssl", "false");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=" + dbSsl, dbUser, dbPass);
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the mceconomy table if it does not already exist in the database.
     *
     * @throws SQLException If an error occurs during table creation.
     */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mceconomy (" +
                     "player_uuid VARCHAR(36) PRIMARY KEY NOT NULL, " +
                     "coin BIGINT NOT NULL DEFAULT 0, " +
                     "copper BIGINT NOT NULL DEFAULT 0, " +
                     "silver BIGINT NOT NULL DEFAULT 0, " +
                     "gold BIGINT NOT NULL DEFAULT 0)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Inserts a player into the database if they do not already exist.
     * Uses INSERT IGNORE to handle existing primary keys gracefully.
     *
     * @param playerUuid The UUID of the player to verify.
     */
    @Override
    public void ensurePlayerExist(String playerUuid) {
        String sql = "INSERT IGNORE INTO mceconomy (player_uuid) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
     */
    @Override
    public void setCoin(String playerUuid, String coinType, int amount) {
        ensurePlayerExist(playerUuid);
        String sql = "UPDATE mceconomy SET " + coinType + " = ? WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, playerUuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds an amount to the player's current balance for a specific coin type.
     *
     * @param playerUuid The UUID of the player.
     * @param coinType   The column name.
     * @param amount     The amount to add.
     */
    @Override
    public void addCoin(String playerUuid, String coinType, int amount) {
        setCoin(playerUuid, coinType, getCoin(playerUuid, coinType) + amount);
    }

    /**
     * Subtracts an amount from the player's current balance for a specific coin type.
     * Prevents the balance from dropping below 0.
     *
     * @param playerUuid The UUID of the player.
     * @param coinType   The column name.
     * @param amount     The amount to subtract.
     */
    @Override
    public void minusCoin(String playerUuid, String coinType, int amount) {
        int currentBalance = getCoin(playerUuid, coinType);
        if (currentBalance >= amount) {
            setCoin(playerUuid, coinType, currentBalance - amount);
        } else {
            Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
            if (player != null) {
                player.sendMessage(Component.text("You do not have enough " + coinType + "!", NamedTextColor.RED));
            }
        }
    }

    /**
     * Transfers an amount of a specific coin type from one player to another.
     * Verifies that the sender has enough balance before proceeding.
     *
     * @param senderUuid   The UUID of the player sending the coins.
     * @param receiverUuid The UUID of the player receiving the coins.
     * @param coinType     The column name.
     * @param amount       The amount to transfer.
     */
    @Override
    public void sendCoin(String senderUuid, String receiverUuid, String coinType, int amount) {
        int senderBalance = getCoin(senderUuid, coinType);
        if (senderBalance >= amount) {
            minusCoin(senderUuid, coinType, amount);
            addCoin(receiverUuid, coinType, amount);
        } else {
            Player sender = Bukkit.getPlayer(UUID.fromString(senderUuid));
            if (sender != null) {
                sender.sendMessage(Component.text("Transfer failed: Not enough " + coinType + "!", NamedTextColor.RED));
            }
        }
    }

    /**
     * Closes the MySQL connection and releases resources.
     */
    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}