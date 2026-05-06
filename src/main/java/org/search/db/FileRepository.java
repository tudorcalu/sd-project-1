package org.search.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class FileRepository {
    private FileRepository() {
    }

    static void insertOrUpdateFile(String path, String content, String metadata, long lastModified, double pathScore, long fileSize) {
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
            TermIndexRepository.rebuildTermIndex(conn, path, content);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Map<String, QueryHandler.FileIndexState> getIndexedFileStatesUnderRoot(String rootDirectory) {
        String sql = "SELECT path, last_modified, file_size FROM files WHERE path LIKE ?";
        Map<String, QueryHandler.FileIndexState> states = new HashMap<>();
        String normalizedRoot = rootDirectory.endsWith("/") || rootDirectory.endsWith("\\")
                ? rootDirectory
                : rootDirectory + java.io.File.separator;
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedRoot + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    states.put(
                            rs.getString("path"),
                            new QueryHandler.FileIndexState(rs.getLong("last_modified"), rs.getLong("file_size"))
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return states;
    }

    static int deleteFilesMissingFromSnapshot(String rootDirectory, Set<String> seenPaths) {
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

    static List<QueryHandler.FileRecord> getFilesByPaths(Set<String> paths, int limit) {
        if (paths.isEmpty()) {
            return List.of();
        }
        List<String> orderedPaths = new ArrayList<>(paths);
        if (orderedPaths.size() > limit) {
            orderedPaths = orderedPaths.subList(0, limit);
        }
        String placeholders = orderedPaths.stream().map(p -> "?").collect(Collectors.joining(","));
        String sql = "SELECT path, content, last_modified, path_score FROM files WHERE path IN (" + placeholders + ")";
        List<QueryHandler.FileRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < orderedPaths.size(); i++) {
                stmt.setString(i + 1, orderedPaths.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new QueryHandler.FileRecord(
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

    static Set<String> getPathsMatchingAllPathTerms(List<String> pathTerms) {
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

    static Set<String> getAllIndexedPaths() {
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
}
