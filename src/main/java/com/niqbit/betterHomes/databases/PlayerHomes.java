package com.niqbit.betterHomes.databases;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;


public class PlayerHomes {
    private static Connection connection;
    private static JavaPlugin plugin;

    public PlayerHomes(JavaPlugin plugin) {
        PlayerHomes.plugin = plugin;
    }

    public void connect() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "homes.db");

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        try {
            connection = DriverManager.getConnection(url);
            initTables();
            plugin.getLogger().info("Database sucessfully connected and initialised");
        } catch (SQLException e) {
            plugin.getLogger().severe("Database failed to connect");
        }

    }

    public void initTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_homes (
                    uuid        TEXT NOT NULL,
                    home_name   TEXT NOT NULL COLLATE NOCASE,
                    world       TEXT NOT NULL,
                    x           REAL NOT NULL,
                    y           REAL NOT NULL,
                    z           REAL NOT NULL,
                    yaw         REAL NOT NULL,
                    pitch       REAL NOT NULL,
                    PRIMARY KEY (uuid, home_name))
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while initializing Tables: " + e.getMessage());
        }
    }

    public static int countHomes(UUID uuid) {
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT COUNT(*) FROM player_homes WHERE uuid = ?")) {
            pstmt.setString(1, String.valueOf(uuid));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while counting Homes for player " + uuid + ": " + e.getMessage());
            return -1;
        }
        return -1;
    }

    public static List<String> getHomes(UUID uuid) {
        List<String> homes = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT home_name FROM player_homes WHERE uuid = ?")) {
            pstmt.setString(1, String.valueOf(uuid));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                homes.add(rs.getString("home_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while getting Homes for player " + uuid + ": " + e.getMessage());
        }
        return homes;
    }

    public static Map<String, Object> getHome(UUID uuid, String homeName) {
        try(PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM player_homes WHERE uuid = ? AND home_name = ?")) {
            pstmt.setString(1, String.valueOf(uuid));
            pstmt.setString(2, homeName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> home = new HashMap<>();
                home.put("world", rs.getString("world"));
                home.put("x", rs.getDouble("x"));
                home.put("y", rs.getDouble("y"));
                home.put("z", rs.getDouble("z"));
                home.put("yaw", rs.getFloat("yaw"));
                home.put("pitch", rs.getFloat("pitch"));
                return home;
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while grabbing Home for player " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    public static int createHome(UUID suuid, String homename, World sworld, double x, double y, double z, float yaw, float pitch) {
        String uuid = String.valueOf(suuid);
        String world = sworld.getName();
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO player_homes (uuid, home_name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, homename);
            pstmt.setString(3, world);
            pstmt.setDouble(4, x);
            pstmt.setDouble(5, y);
            pstmt.setDouble(6, z);
            pstmt.setFloat(7, yaw);
            pstmt.setFloat(8, pitch);
            pstmt.executeUpdate();
            return 1;
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while adding your Home: " + e.getMessage());
            return -1;
        }
    }

    public static boolean homeExists(UUID uuid, String homeName) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT 1 FROM player_homes WHERE uuid = ? AND home_name = ? LIMIT 1")) {
            pstmt.setString(1, String.valueOf(uuid));
            pstmt.setString(2, homeName);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while checking if home exists for player " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public static int deleteHome(UUID uuid, String homeName) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "DELETE FROM player_homes WHERE uuid = ? AND home_name = ?")) {
            pstmt.setString(1, String.valueOf(uuid));
            pstmt.setString(2, homeName);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0 ? 1 : 0; // Returns 1 if deleted, 0 if nothing was deleted
        } catch (SQLException e) {
            plugin.getLogger().severe("Something went wrong while deleting home for player " + uuid + ": " + e.getMessage());
            return -1;
        }
    }

}
