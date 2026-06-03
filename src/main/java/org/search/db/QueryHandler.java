package org.search.db;

import org.search.search.fts.FtsSearchRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryHandler {

    public static void initializeDatabase() {
        DbSchemaManager.initializeDatabase();
    }

    public static void insertOrUpdateFile(
            String path,
            String content,
            String metadata,
            long lastModified,
            double pathScore,
            long fileSize,
            String fileType,
            String dominantColor) {
        FileRepository.insertOrUpdateFile(path, content, metadata, lastModified, pathScore, fileSize, fileType, dominantColor);
    }

    public static void recordSearchQuery(String query) {
        SearchHistoryRepository.recordSearchQuery(query);
    }

    public static List<String> getSuggestions(String prefix, int limit) {
        return SearchHistoryRepository.getSuggestions(prefix, limit);
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

    public static Set<String> getPathsMatchingAllColors(List<String> colorTerms) {
        return FileRepository.getPathsMatchingAllColors(colorTerms);
    }

    public static Set<String> ftsSearchPaths(String ftsQuery, int limit) {
        return FtsSearchRepository.searchPaths(ftsQuery, limit);
    }

    public static Set<String> getAllIndexedPaths() {
        return FileRepository.getAllIndexedPaths();
    }

    public static List<FileRecord> getFilesByPaths(Set<String> paths, int limit) {
        return FileRepository.getFilesByPaths(paths, limit);
    }

    public static void recordResultHits(String query, List<String> paths) {
        SearchHistoryRepository.recordResultHits(query, paths);
    }

    public static void reindexFullText() {
        FtsIndexManager.reindexAll();
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
        private final String fileType;
        private final String dominantColor;

        public FileRecord(
                String path,
                String content,
                long lastModified,
                double pathScore,
                String fileType,
                String dominantColor) {
            this.path = path;
            this.content = content;
            this.lastModified = lastModified;
            this.pathScore = pathScore;
            this.fileType = fileType;
            this.dominantColor = dominantColor;
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

        public String getFileType() {
            return fileType;
        }

        public String getDominantColor() {
            return dominantColor;
        }
    }
}
