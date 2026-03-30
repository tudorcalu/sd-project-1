package org.search.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryHandler {

    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "path VARCHAR(1024) UNIQUE, " +
                "content TEXT, " +
                "metadata TEXT, " +
                "last_modified BIGINT)";
        
        try (Connection conn = DatabaseContext.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertOrUpdateFile(String path, String content, String metadata, long lastModified) {
        String sql = "MERGE INTO files (path, content, metadata, last_modified) KEY(path) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, path);
            pstmt.setString(2, content);
            pstmt.setString(3, metadata);
            pstmt.setLong(4, lastModified);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
