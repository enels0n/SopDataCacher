package net.enelson.sopdatacacher.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLManager {
    private final Plugin plugin;
    private Connection conn;

    public SQLManager(Plugin plugin) {
        this.plugin = plugin;
        this.conn = getConn();
        initDatabase();
        sanitizeDatabase();
    }

    private Connection getConn() {
        try {
            String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db";
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return null;
        }
    }

    private void initDatabase() {
        try {
            if (conn == null || conn.isClosed()) conn = getConn();
            String query = "CREATE TABLE IF NOT EXISTS players (" +
                    "player TEXT PRIMARY KEY, " +
                    "data TEXT NOT NULL DEFAULT '{}'" +
                    ")";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sanitizeDatabase() {
        try {
            if (conn == null || conn.isClosed()) conn = getConn();
            String query = "UPDATE players SET data = '{}' WHERE data IS NULL OR json_valid(data) = 0 OR data = ''";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerDataField(String player, String key, String value) {
        try {
            if (conn == null || conn.isClosed()) conn = getConn();

            String insertIfNotExist = "INSERT OR IGNORE INTO players (player, data) VALUES (?, '{}')";
            PreparedStatement insertStmt = conn.prepareStatement(insertIfNotExist);
            insertStmt.setString(1, player);
            insertStmt.executeUpdate();
            insertStmt.close();

            Object finalValueObj = value;
            try {
                if (value != null && value.matches("^-?\\d+(\\.\\d+)?$")) {
                    if (value.contains(".")) {
                        finalValueObj = Double.parseDouble(value);
                    } else {
                        finalValueObj = Long.parseLong(value);
                    }
                }
            } catch (Exception ignored) {
            }

            String jsonPath = "$." + key;
            String updateQuery;
            if (finalValueObj instanceof Number) {
                updateQuery = "UPDATE players SET data = json_set(data, '" + jsonPath + "', " + value + ") WHERE player = ?";
            } else {
                updateQuery = "UPDATE players SET data = json_set(data, '" + jsonPath + "', ?) WHERE player = ?";
            }

            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            if (finalValueObj instanceof Number) {
                updateStmt.setString(1, player);
            } else {
                updateStmt.setString(1, value);
                updateStmt.setString(2, player);
            }
            updateStmt.executeUpdate();
            updateStmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerDataField(String player, String key) {
        String value = null;
        try {
            if (conn == null || conn.isClosed()) conn = getConn();
            String jsonPath = "$." + key;
            String query = "SELECT json_extract(data, ?) AS value FROM players WHERE player = ? AND json_valid(data) = 1";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, jsonPath);
            statement.setString(2, player);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                value = rs.getString("value");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return value;
    }

    public ResultSet getTop10ByField(String key) {
        try {
            if (conn == null || conn.isClosed()) conn = getConn();
            String jsonPath = "$." + key;
            String query =
                    "SELECT player, json_extract(data, ?) AS value FROM players " +
                    "WHERE json_valid(data) = 1 " +
                    "AND json_type(data, ?) IN ('integer', 'real') " +
                    "ORDER BY CAST(json_extract(data, ?) AS REAL) DESC LIMIT 10";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, jsonPath);
            statement.setString(2, jsonPath);
            statement.setString(3, jsonPath);
            return statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void disconnect() {
        try {
            if (conn != null) conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
