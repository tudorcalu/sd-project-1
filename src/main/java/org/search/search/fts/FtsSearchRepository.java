package org.search.search.fts;

import org.h2.fulltext.FullText;
import org.search.db.DatabaseContext;
import org.search.db.FtsIndexManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public final class FtsSearchRepository {
    private FtsSearchRepository() {
    }

    public static Set<String> searchPaths(String ftsQuery, int limit) {
        if (ftsQuery == null || ftsQuery.isBlank()) {
            return Set.of();
        }
        FtsIndexManager.initialize();
        Set<String> paths = new HashSet<>();
        try {
            Connection conn = DatabaseContext.getJdbcConnection();
            ResultSet rs = FullText.searchData(conn, ftsQuery.trim(), limit, 0);
            while (rs.next()) {
                String path = resolvePathFromKeys(conn, rs.getObject("KEYS"));
                if (path != null) {
                    paths.add(path);
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static String resolvePathFromKeys(Connection conn, Object keysObject) throws SQLException {
        if (keysObject == null) {
            return null;
        }
        String idValue;
        if (keysObject instanceof String[] keys && keys.length > 0) {
            idValue = keys[0];
        } else {
            idValue = keysObject.toString();
        }
        if (idValue == null || idValue.isBlank()) {
            return null;
        }
        return lookupPathById(conn, Integer.parseInt(idValue.trim()));
    }

    private static String lookupPathById(Connection conn, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT path FROM files WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("path");
                }
            }
        }
        return null;
    }
}
