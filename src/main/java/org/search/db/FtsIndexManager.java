package org.search.db;

import org.h2.fulltext.FullText;

import java.sql.Connection;
import java.sql.SQLException;

public final class FtsIndexManager {
    private static final String FTS_COLUMNS = "PATH, CONTENT";
    private static volatile boolean initialized;

    private FtsIndexManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (FtsIndexManager.class) {
            if (initialized) {
                return;
            }
            try {
                Connection conn = DatabaseContext.getJdbcConnection();
                FullText.init(conn);
                createIndexIfNeeded(conn);
                initialized = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createIndexIfNeeded(Connection conn) throws SQLException {
        try {
            FullText.createIndex(conn, "PUBLIC", "FILES", FTS_COLUMNS);
        } catch (SQLException e) {
            if (isIndexAlreadyExists(e)) {
                return;
            }
            throw e;
        }
    }

    private static boolean isIndexAlreadyExists(SQLException e) {
        if (e.getErrorCode() == 23505) {
            return true;
        }
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("already") || message.contains("exists")
                || message.contains("unique index") || message.contains("primary key violation");
    }

    public static void reindexAll() {
        initialize();
        try {
            Connection conn = DatabaseContext.getJdbcConnection();
            FullText.reindex(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
