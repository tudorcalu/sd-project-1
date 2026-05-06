package org.search.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class DbSchemaManager {
    private DbSchemaManager() {
    }

    static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "path VARCHAR(1024) UNIQUE, " +
                "content TEXT, " +
                "metadata TEXT, " +
                "last_modified BIGINT, " +
                "path_score DOUBLE DEFAULT 0, " +
                "file_size BIGINT DEFAULT 0)";
        String createHistorySQL = "CREATE TABLE IF NOT EXISTS search_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "query VARCHAR(2048) UNIQUE, " +
                "usage_count INT DEFAULT 0, " +
                "last_executed BIGINT DEFAULT 0)";
        String createResultHitsSQL = "CREATE TABLE IF NOT EXISTS search_result_hits (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "query VARCHAR(2048), " +
                "path VARCHAR(1024), " +
                "hit_count INT DEFAULT 0, " +
                "last_seen BIGINT DEFAULT 0, " +
                "UNIQUE(query, path))";
        String createTermIndexSQL = "CREATE TABLE IF NOT EXISTS term_index (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "term VARCHAR(128), " +
                "path VARCHAR(1024), " +
                "field VARCHAR(16), " +
                "term_frequency INT DEFAULT 1, " +
                "UNIQUE(term, path, field), " +
                "FOREIGN KEY(path) REFERENCES files(path) ON DELETE CASCADE)";
        String alterPathScore = "ALTER TABLE files ADD COLUMN IF NOT EXISTS path_score DOUBLE DEFAULT 0";
        String alterFileSize = "ALTER TABLE files ADD COLUMN IF NOT EXISTS file_size BIGINT DEFAULT 0";
        String createFilesLastModifiedIdx = "CREATE INDEX IF NOT EXISTS idx_files_last_modified ON files(last_modified)";
        String createFilesPathScoreIdx = "CREATE INDEX IF NOT EXISTS idx_files_path_score ON files(path_score)";
        String createHistoryQueryIdx = "CREATE INDEX IF NOT EXISTS idx_search_history_last_executed ON search_history(last_executed)";
        String createHitsQueryIdx = "CREATE INDEX IF NOT EXISTS idx_search_result_hits_query ON search_result_hits(query)";
        String createTermIdx = "CREATE INDEX IF NOT EXISTS idx_term_index_term_field ON term_index(term, field)";
        String createTermPathIdx = "CREATE INDEX IF NOT EXISTS idx_term_index_path ON term_index(path)";

        try (Connection conn = DatabaseContext.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(alterPathScore);
            stmt.execute(alterFileSize);
            stmt.execute(createHistorySQL);
            stmt.execute(createResultHitsSQL);
            stmt.execute(createTermIndexSQL);
            stmt.execute(createFilesLastModifiedIdx);
            stmt.execute(createFilesPathScoreIdx);
            stmt.execute(createHistoryQueryIdx);
            stmt.execute(createHitsQueryIdx);
            stmt.execute(createTermIdx);
            stmt.execute(createTermPathIdx);
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
