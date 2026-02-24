package io.github.mcengine.mceconomy.common.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mceconomy.api.enums.CurrencyType;
import org.bukkit.plugin.Plugin;
import java.sql.*;
import java.util.Objects;

/**
 * MySQL implementation for MCEconomy.
 */
public class MCEconomyMySQL implements IMCEconomyDB {
    /**
     * The connection pool data source.
     */
    private final HikariDataSource dataSource;

    /**
     * Constructs a new MySQL database handler.
     * Initializes connection settings from the plugin configuration and attempts to connect.
     *
     * @param plugin The Bukkit/Spigot plugin instance.
     */
    public MCEconomyMySQL(Plugin plugin) {
        String dbUser = envOrConfig("MCENGINE_MCECONOMY_MYSQL_USER", "MCENGINE_MYSQL_USER", "db.mysql.user", plugin, null);
        String dbPass = envOrConfig("MCENGINE_MCECONOMY_MYSQL_PASS", "MCENGINE_MYSQL_PASS", "db.mysql.password", plugin, null);
        String dbHost = envOrConfig("MCENGINE_MCECONOMY_MYSQL_HOST", "MCENGINE_MYSQL_HOST", "db.mysql.host", plugin, null);
        String dbName = envOrConfig("MCENGINE_MCECONOMY_MYSQL_DATABASE_NAME", "MCENGINE_MYSQL_DATABASE_NAME", "db.mysql.database", plugin, null);
        String dbPort = envOrConfig("MCENGINE_MCECONOMY_MYSQL_PORT", "MCENGINE_MYSQL_PORT", "db.mysql.port", plugin, "3306");
        String dbSsl = envOrConfig("MCENGINE_MCECONOMY_MYSQL_SSL", "MCENGINE_MYSQL_SSL", "db.mysql.ssl", plugin, "false");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=" + dbSsl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);
        
        // Pool Settings optimized for Minecraft
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); 
        config.setLeakDetectionThreshold(10000);
        
        // Performance properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        try {
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolve configuration by preferring environment variables then falling back to plugin config.
     */
    private String envOrConfig(String primaryEnv, String secondaryEnv, String configPath, Plugin plugin, String defaultValue) {
        String value = System.getenv(primaryEnv);
        if (value != null && !value.isEmpty()) return value;

        value = System.getenv(secondaryEnv);
        if (value != null && !value.isEmpty()) return value;

        value = plugin.getConfig().getString(configPath);
        if (value != null && !value.isEmpty()) return value;

        return defaultValue;
    }

    /**
     * Creates the economy_accounts table if it does not already exist.
     * Uses a composite primary key (account_uuid + account_type).
     *
     * @throws SQLException If an error occurs during table creation.
     */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS economy_accounts (" +
                     "account_uuid VARCHAR(36) NOT NULL, " +
                     "account_type VARCHAR(32) NOT NULL, " +
                     "coin BIGINT NOT NULL DEFAULT 0, " +
                     "copper BIGINT NOT NULL DEFAULT 0, " +
                     "silver BIGINT NOT NULL DEFAULT 0, " +
                     "gold BIGINT NOT NULL DEFAULT 0, " +
                     "PRIMARY KEY (account_uuid, account_type))";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Inserts an account into the database if it does not already exist.
     * Uses INSERT IGNORE to handle existing primary keys gracefully.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @return true if the operation executed successfully, false if a SQL error occurred.
     */
    @Override
    public boolean ensureAccountExist(String accountUuid, String accountType) {
        String sql = "INSERT IGNORE INTO economy_accounts (account_uuid, account_type) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountUuid);
            pstmt.setString(2, accountType);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the balance of a specific coin type for an account.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @return The amount found in the database, or 0 if an error occurs.
     */
    @Override
    public int getCoin(String accountUuid, String accountType, CurrencyType coinType) {
        String col = columnName(coinType);
        ensureAccountExist(accountUuid, accountType);
        String sql = "SELECT " + col + " FROM economy_accounts WHERE account_uuid = ? AND account_type = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountUuid);
            pstmt.setString(2, accountType);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Sets the balance of a specific coin type for an account to a specific amount.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @param amount      The new value to set.
     * @return true if the update was successful, false on error.
     */
    @Override
    public boolean setCoin(String accountUuid, String accountType, CurrencyType coinType, int amount) {
        if (amount < 0) return false;
        String col = columnName(coinType);
        ensureAccountExist(accountUuid, accountType);
        String sql = "UPDATE economy_accounts SET " + col + " = ? WHERE account_uuid = ? AND account_type = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, accountUuid);
            pstmt.setString(3, accountType);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds an amount to the account's current balance for a specific coin type.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @param amount      The amount to add.
     * @return true if successful, false on error.
     */
    @Override
    public boolean addCoin(String accountUuid, String accountType, CurrencyType coinType, int amount) {
        if (amount <= 0) return false;
        ensureAccountExist(accountUuid, accountType);
        String col = columnName(coinType);
        String sql = "UPDATE economy_accounts SET " + col + " = " + col + " + ? WHERE account_uuid = ? AND account_type = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, accountUuid);
            pstmt.setString(3, accountType);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Subtracts an amount from the account's current balance for a specific coin type.
     * Prevents the balance from dropping below 0.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @param amount      The amount to subtract.
     * @return true if transaction succeeded, false if insufficient funds or error.
     */
    @Override
    public boolean minusCoin(String accountUuid, String accountType, CurrencyType coinType, int amount) {
        if (amount <= 0) return false;
        String col = columnName(coinType);
        String sql = "UPDATE economy_accounts SET " + col + " = " + col + " - ? " +
                     "WHERE account_uuid = ? AND account_type = ? AND " + col + " >= ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, accountUuid);
            pstmt.setString(3, accountType);
            pstmt.setInt(4, amount);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Transfers an amount of a specific coin type from one account to another.
     *
     * @param senderUuid   The UUID of the sender.
     * @param senderType   The account type of the sender.
     * @param receiverUuid The UUID of the receiver.
     * @param receiverType The account type of the receiver.
     * @param coinType     The currency type.
     * @param amount       The amount to transfer.
     * @return true if transfer succeeded, false if sender has insufficient funds.
     */
    @Override
    public boolean sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, CurrencyType coinType, int amount) {
        if (amount <= 0) return false;
        String col = columnName(coinType);
        try (Connection conn = dataSource.getConnection()) {
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                ensureAccountExist(senderUuid, senderType);
                ensureAccountExist(receiverUuid, receiverType);

                String withdrawSql = "UPDATE economy_accounts SET " + col + " = " + col + " - ? " +
                                     "WHERE account_uuid = ? AND account_type = ? AND " + col + " >= ?";
                try (PreparedStatement withdraw = conn.prepareStatement(withdrawSql)) {
                    withdraw.setInt(1, amount);
                    withdraw.setString(2, senderUuid);
                    withdraw.setString(3, senderType);
                    withdraw.setInt(4, amount);
                    if (withdraw.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                String depositSql = "UPDATE economy_accounts SET " + col + " = " + col + " + ? WHERE account_uuid = ? AND account_type = ?";
                try (PreparedStatement deposit = conn.prepareStatement(depositSql)) {
                    deposit.setInt(1, amount);
                    deposit.setString(2, receiverUuid);
                    deposit.setString(3, receiverType);
                    deposit.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Resolve the trusted column name for a currency type.
     */
    private String columnName(CurrencyType type) {
        Objects.requireNonNull(type, "currency type");
        return switch (type) {
            case COIN -> "coin";
            case COPPER -> "copper";
            case SILVER -> "silver";
            case GOLD -> "gold";
        };
    }

    /**
     * Closes the MySQL connection pool and releases resources.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
