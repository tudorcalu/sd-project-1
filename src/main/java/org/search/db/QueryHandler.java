package org.search.db;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryHandler {

    public static void initializeDatabase() {
        DbSchemaManager.initializeDatabase();
    }

    public static void insertOrUpdateFile(String path, String content, String metadata, long lastModified, double pathScore, long fileSize) {
        FileRepository.insertOrUpdateFile(path, content, metadata, lastModified, pathScore, fileSize);
    }

    public static void recordSearchQuery(String query) {
        SearchHistoryRepository.recordSearchQuery(query);
    }

    public static List<String> getSuggestions(String prefix, int limit) {
        return SearchHistoryRepository.getSuggestions(prefix, limit);
    }

    public static void recordResultHit(String query, String path) {
        SearchHistoryRepository.recordResultHit(query, path);
    }

    public static Map<String, Integer> getResultHitCounts(String query) {
        return SearchHistoryRepository.getResultHitCounts(query);
    }

    public static Map<String, FileIndexState> getIndexedFileStatesUnderRoot(String rootDirectory) {
        return FileRepository.getIndexedFileStatesUnderRoot(rootDirectory);
    }

    public static int deleteFilesMissingFromSnapshot(String rootDirectory, Set<String> seenPaths) {
        return FileRepository.deleteFilesMissingFromSnapshot(rootDirectory, seenPaths);
    }

    public static Set<String> getPathsMatchingAllPathTerms(List<String> pathTerms) {
        return FileRepository.getPathsMatchingAllPathTerms(pathTerms);
    }

    public static Set<String> getPathsContainingAllPathTokens(List<String> tokens) {
        return TermIndexRepository.getPathsContainingAllTokens(tokens, "path");
    }

    public static Set<String> getPathsMatchingTokenWithTypos(String token, String field) {
        return TermIndexRepository.getPathsMatchingTokenWithTypos(token, field);
    }

    public static Set<String> getPathsContainingAllContentTokens(List<String> tokens) {
        return TermIndexRepository.getPathsContainingAllTokens(tokens, "content");
    }

    public static Set<String> getPathsMatchingFreeToken(String token) {
        Set<String> matches = TermIndexRepository.getPathsContainingAnyTokens(List.of(token), "path");
        matches.addAll(TermIndexRepository.getPathsContainingAnyTokens(List.of(token), "content"));
        return matches;
    }

    public static List<FileRecord> getFilesByPaths(Set<String> paths, int limit) {
        return FileRepository.getFilesByPaths(paths, limit);
    }

    public static void recordResultHits(String query, List<String> paths) {
        SearchHistoryRepository.recordResultHits(query, paths);
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
