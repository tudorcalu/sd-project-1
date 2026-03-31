package org.search.controller;

import org.search.db.DatabaseContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SearchController {

    public List<SearchResult> search(String queryStr) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT path, content FROM files WHERE path LIKE ? OR content LIKE ? LIMIT 100";
        
        try (Connection conn = DatabaseContext.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            String term = "%" + queryStr + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String path = rs.getString("path");
                    String content = rs.getString("content");
                    
                    String snippet = "";
                    if (content != null) {
                        int idx = content.toLowerCase().indexOf(queryStr.toLowerCase());
                        if (idx != -1) {
                            int start = Math.max(0, idx - 40);
                            int end = Math.min(content.length(), idx + queryStr.length() + 80);
                            snippet = "..." + content.substring(start, end).replace('\n', ' ') + "...";
                        } else {
                            snippet = content.substring(0, Math.min(content.length(), 100)).replace('\n', ' ') + "...";
                        }
                    }
                    
                    results.add(new SearchResult(path, snippet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static class SearchResult {
        private final String path;
        private final String snippet;

        public SearchResult(String path, String snippet) {
            this.path = path;
            this.snippet = snippet;
        }

        public String getPath() { return path; }
        public String getSnippet() { return snippet; }
        
        @Override
        public String toString() {
            return "File: " + path + "\nSnippet: " + snippet + "\n";
        }
    }
}
