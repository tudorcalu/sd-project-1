package org.search.db;

import org.search.parser.Tokenizer;
import org.search.search.TypoMatcher;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryHandler {

    public static void initializeDatabase() {
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

    public static void insertOrUpdateFile(String path, String content, String metadata, long lastModified, double pathScore, long fileSize) {
        String sql = "MERGE INTO files (path, content, metadata, last_modified, path_score, file_size) KEY(path) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, path);
            pstmt.setString(2, content);
            pstmt.setString(3, metadata);
            pstmt.setLong(4, lastModified);
            pstmt.setDouble(5, pathScore);
            pstmt.setLong(6, fileSize);
            pstmt.executeUpdate();

            rebuildTermIndex(conn, path, content);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void recordSearchQuery(String query) {
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

    public static List<String> getSuggestions(String prefix, int limit) {
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

    public static void recordResultHit(String query, String path) {
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

    public static Map<String, Integer> getResultHitCounts(String query) {
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

    public static Map<String, FileIndexState> getIndexedFileStatesUnderRoot(String rootDirectory) {
        String sql = "SELECT path, last_modified, file_size FROM files WHERE path LIKE ?";
        Map<String, FileIndexState> states = new HashMap<>();
        String normalizedRoot = rootDirectory.endsWith("/") || rootDirectory.endsWith("\\")
                ? rootDirectory
                : rootDirectory + java.io.File.separator;
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedRoot + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String path = rs.getString("path");
                    long lastModified = rs.getLong("last_modified");
                    long fileSize = rs.getLong("file_size");
                    states.put(path, new FileIndexState(lastModified, fileSize));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return states;
    }

    public static int deleteFilesMissingFromSnapshot(String rootDirectory, Set<String> seenPaths) {
        String sql = "SELECT path FROM files WHERE path LIKE ?";
        String normalizedRoot = rootDirectory.endsWith("/") || rootDirectory.endsWith("\\")
                ? rootDirectory
                : rootDirectory + java.io.File.separator;
        Set<String> stalePaths = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedRoot + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String path = rs.getString("path");
                    if (!seenPaths.contains(path)) {
                        stalePaths.add(path);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        if (stalePaths.isEmpty()) {
            return 0;
        }

        String deleteSql = "DELETE FROM files WHERE path = ?";
        int deleted = 0;
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            for (String path : stalePaths) {
                deleteStmt.setString(1, path);
                deleted += deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deleted;
    }

    public static Set<String> getPathsMatchingAllPathTerms(List<String> pathTerms) {
        if (pathTerms.isEmpty()) {
            return getAllIndexedPaths();
        }

        StringBuilder sql = new StringBuilder("SELECT path FROM files WHERE 1=1");
        for (int i = 0; i < pathTerms.size(); i++) {
            sql.append(" AND LOWER(path) LIKE ?");
        }

        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String term : pathTerms) {
                stmt.setString(idx++, "%" + term.toLowerCase() + "%");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static Set<String> getPathsContainingAllPathTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return getAllIndexedPaths();
        }

        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT path FROM term_index WHERE field = 'path' AND term IN (" + placeholders + ") " +
                "GROUP BY path HAVING COUNT(DISTINCT term) = ?";

        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String token : tokens) {
                stmt.setString(idx++, token);
            }
            stmt.setInt(idx, tokens.size());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static Set<String> getPathsMatchingTokenWithTypos(String token, String field) {
        if (token == null || token.isBlank()) {
            return Set.of();
        }
        String normalized = token.toLowerCase();
        Set<String> matchedTerms = getSimilarTerms(normalized, field);
        if (matchedTerms.isEmpty()) {
            matchedTerms.add(normalized);
        }
        return getPathsForTerms(matchedTerms, field);
    }

    public static Set<String> getPathsContainingAllContentTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return getAllIndexedPaths();
        }

        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT path FROM term_index WHERE field = 'content' AND term IN (" + placeholders + ") " +
                "GROUP BY path HAVING COUNT(DISTINCT term) = ?";

        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String token : tokens) {
                stmt.setString(idx++, token);
            }
            stmt.setInt(idx, tokens.size());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static Set<String> getPathsMatchingFreeToken(String token) {
        Set<String> matches = new HashSet<>();
        matches.addAll(getPathsContainingAnyPathTokens(List.of(token)));
        matches.addAll(getPathsContainingAnyContentTokens(List.of(token)));
        return matches;
    }

    public static List<FileRecord> getFilesByPaths(Set<String> paths, int limit) {
        if (paths.isEmpty()) {
            return List.of();
        }

        List<String> orderedPaths = new ArrayList<>(paths);
        if (orderedPaths.size() > limit) {
            orderedPaths = orderedPaths.subList(0, limit);
        }
        String placeholders = orderedPaths.stream().map(p -> "?").collect(Collectors.joining(","));
        String sql = "SELECT path, content, last_modified, path_score FROM files WHERE path IN (" + placeholders + ")";

        List<FileRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < orderedPaths.size(); i++) {
                stmt.setString(i + 1, orderedPaths.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new FileRecord(
                            rs.getString("path"),
                            rs.getString("content"),
                            rs.getLong("last_modified"),
                            rs.getDouble("path_score")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    private static Set<String> getAllIndexedPaths() {
        String sql = "SELECT path FROM files";
        Set<String> paths = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                paths.add(rs.getString("path"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static Set<String> getPathsContainingAnyContentTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return Set.of();
        }
        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT path FROM term_index WHERE field = 'content' AND term IN (" + placeholders + ")";
        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String token : tokens) {
                stmt.setString(idx++, token);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    private static Set<String> getPathsContainingAnyPathTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return Set.of();
        }
        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT path FROM term_index WHERE field = 'path' AND term IN (" + placeholders + ")";
        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String token : tokens) {
                stmt.setString(idx++, token);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Finds indexed terms within edit distance of {@code token}. Uses length banding only (no single-letter
     * prefix + low LIMIT), which incorrectly dropped candidates like "architecture" for "archtecture".
     */
    private static Set<String> getSimilarTerms(String token, String field) {
        int maxDistance = TypoMatcher.maxDistanceFor(token);
        if (maxDistance == 0) {
            return Set.of(token);
        }

        // Fast path: exact hit already in the index.
        if (termExistsInIndex(token, field)) {
            return Set.of(token);
        }

        int minLen = Math.max(1, token.length() - maxDistance);
        int maxLen = token.length() + maxDistance;
        // Upper bound so typos with wrong first letter are still discoverable (prefix LIKE 'a%' was too narrow).
        final int maxCandidates = 100_000;

        String sql = "SELECT DISTINCT term FROM term_index " +
                "WHERE field = ? AND LENGTH(term) BETWEEN ? AND ? LIMIT ?";
        Set<String> similarTerms = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, field);
            stmt.setInt(2, minLen);
            stmt.setInt(3, maxLen);
            stmt.setInt(4, maxCandidates);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String candidate = rs.getString("term");
                    if (TypoMatcher.isTypoMatch(token, candidate)) {
                        similarTerms.add(candidate);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (similarTerms.isEmpty()) {
            similarTerms.add(token);
        }
        return similarTerms;
    }

    private static boolean termExistsInIndex(String term, String field) {
        String sql = "SELECT 1 FROM term_index WHERE field = ? AND term = ? LIMIT 1";
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, field);
            stmt.setString(2, term);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Set<String> getPathsForTerms(Set<String> terms, String field) {
        if (terms.isEmpty()) {
            return Set.of();
        }
        List<String> orderedTerms = new ArrayList<>(terms);
        String placeholders = orderedTerms.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT path FROM term_index WHERE field = ? AND term IN (" + placeholders + ")";
        Set<String> paths = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, field);
            int idx = 2;
            for (String term : orderedTerms) {
                stmt.setString(idx++, term);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    paths.add(rs.getString("path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static void rebuildTermIndex(Connection conn, String path, String content) throws SQLException {
        try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM term_index WHERE path = ?")) {
            deleteStmt.setString(1, path);
            deleteStmt.executeUpdate();
        }

        Map<String, Integer> pathFrequencies = countTerms(Tokenizer.tokenize(path));
        Map<String, Integer> contentFrequencies = countTerms(Tokenizer.tokenize(content));

        String insertSql = "INSERT INTO term_index (term, path, field, term_frequency) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            for (Map.Entry<String, Integer> entry : pathFrequencies.entrySet()) {
                insertStmt.setString(1, entry.getKey());
                insertStmt.setString(2, path);
                insertStmt.setString(3, "path");
                insertStmt.setInt(4, entry.getValue());
                insertStmt.addBatch();
            }
            for (Map.Entry<String, Integer> entry : contentFrequencies.entrySet()) {
                insertStmt.setString(1, entry.getKey());
                insertStmt.setString(2, path);
                insertStmt.setString(3, "content");
                insertStmt.setInt(4, entry.getValue());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    private static Map<String, Integer> countTerms(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) {
            frequencies.merge(token, 1, Integer::sum);
        }
        return frequencies;
    }

    public static void recordResultHits(String query, List<String> paths) {
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

    public static class FileIndexState {
        private final long lastModified;
        private final long fileSize;

        public FileIndexState(long lastModified, long fileSize) {
            this.lastModified = lastModified;
            this.fileSize = fileSize;
        }

        public long getLastModified() {
            return lastModified;
        }

        public long getFileSize() {
            return fileSize;
        }
    }

    public static class FileRecord {
        private final String path;
        private final String content;
        private final long lastModified;
        private final double pathScore;

        public FileRecord(String path, String content, long lastModified, double pathScore) {
            this.path = path;
            this.content = content;
            this.lastModified = lastModified;
            this.pathScore = pathScore;
        }

        public String getPath() {
            return path;
        }

        public String getContent() {
            return content;
        }

        public long getLastModified() {
            return lastModified;
        }

        public double getPathScore() {
            return pathScore;
        }
    }
}
