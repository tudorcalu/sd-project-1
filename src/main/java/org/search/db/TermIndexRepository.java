package org.search.db;

import org.search.parser.Tokenizer;
import org.search.search.TypoMatcher;

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

final class TermIndexRepository {
    private TermIndexRepository() {
    }

    static void rebuildTermIndex(Connection conn, String path, String content) throws SQLException {
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

    static Set<String> getPathsMatchingTokenWithTypos(String token, String field) {
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

    static Set<String> getPathsContainingAllTokens(List<String> tokens, String field) {
        if (tokens.isEmpty()) {
            return FileRepository.getAllIndexedPaths();
        }
        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT path FROM term_index WHERE field = ? AND term IN (" + placeholders + ") GROUP BY path HAVING COUNT(DISTINCT term) = ?";
        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, field);
            int idx = 2;
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

    static Set<String> getPathsContainingAnyTokens(List<String> tokens, String field) {
        if (tokens.isEmpty()) {
            return Set.of();
        }
        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT path FROM term_index WHERE field = ? AND term IN (" + placeholders + ")";
        Set<String> results = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, field);
            int idx = 2;
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

    private static Set<String> getSimilarTerms(String token, String field) {
        int maxDistance = TypoMatcher.maxDistanceFor(token);
        if (maxDistance == 0) {
            return Set.of(token);
        }
        if (termExistsInIndex(token, field)) {
            return Set.of(token);
        }
        int minLen = Math.max(1, token.length() - maxDistance);
        int maxLen = token.length() + maxDistance;
        String sql = "SELECT DISTINCT term FROM term_index WHERE field = ? AND LENGTH(term) BETWEEN ? AND ? LIMIT ?";
        Set<String> similarTerms = new HashSet<>();
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, field);
            stmt.setInt(2, minLen);
            stmt.setInt(3, maxLen);
            stmt.setInt(4, 100000);
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

    private static Map<String, Integer> countTerms(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) {
            frequencies.merge(token, 1, Integer::sum);
        }
        return frequencies;
    }
}
