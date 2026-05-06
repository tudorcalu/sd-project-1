package org.search.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SearchHistoryRepository {
    private SearchHistoryRepository() {
    }

    static void recordSearchQuery(String query) {
        String updateSql = "UPDATE search_history SET usage_count = usage_count + 1, last_executed = ? WHERE query = ?";
        String insertSql = "INSERT INTO search_history (query, usage_count, last_executed) VALUES (?, 1, ?)";
        long now = System.currentTimeMillis();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setLong(1, now);
            updateStmt.setString(2, query);
            int updatedRows = updateStmt.executeUpdate();
            if (updatedRows == 0) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, query);
                    insertStmt.setLong(2, now);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static List<String> getSuggestions(String prefix, int limit) {
        String sql = "SELECT query FROM search_history WHERE LOWER(query) LIKE ? ORDER BY usage_count DESC, last_executed DESC LIMIT ?";
        List<String> suggestions = new ArrayList<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + prefix.toLowerCase() + "%");
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    suggestions.add(rs.getString("query"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suggestions;
    }

    static void recordResultHit(String query, String path) {
        String updateSql = "UPDATE search_result_hits SET hit_count = hit_count + 1, last_seen = ? WHERE query = ? AND path = ?";
        String insertSql = "INSERT INTO search_result_hits (query, path, hit_count, last_seen) VALUES (?, ?, 1, ?)";
        long now = System.currentTimeMillis();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setLong(1, now);
            updateStmt.setString(2, query);
            updateStmt.setString(3, path);
            int updatedRows = updateStmt.executeUpdate();
            if (updatedRows == 0) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, query);
                    insertStmt.setString(2, path);
                    insertStmt.setLong(3, now);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void recordResultHits(String query, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        String updateSql = "UPDATE search_result_hits SET hit_count = hit_count + 1, last_seen = ? WHERE query = ? AND path = ?";
        String insertSql = "INSERT INTO search_result_hits (query, path, hit_count, last_seen) VALUES (?, ?, 1, ?)";
        long now = System.currentTimeMillis();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            for (String path : paths) {
                updateStmt.setLong(1, now);
                updateStmt.setString(2, query);
                updateStmt.setString(3, path);
                int updatedRows = updateStmt.executeUpdate();
                if (updatedRows == 0) {
                    insertStmt.setString(1, query);
                    insertStmt.setString(2, path);
                    insertStmt.setLong(3, now);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Map<String, Integer> getResultHitCounts(String query) {
        String sql = "SELECT path, hit_count FROM search_result_hits WHERE query = ?";
        Map<String, Integer> hitCounts = new HashMap<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, query);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    hitCounts.put(rs.getString("path"), rs.getInt("hit_count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hitCounts;
    }
}
