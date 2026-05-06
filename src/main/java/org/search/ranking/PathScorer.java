package org.search.ranking;

import java.nio.file.Path;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

public final class PathScorer {
    private static final Map<String, Double> EXTENSION_WEIGHTS = Map.of(
            "java", 10.0,
            "md", 9.0,
            "txt", 8.0,
            "pdf", 7.0,
            "xml", 6.0,
            "yml", 5.0,
            "yaml", 5.0
    );
    private static final Set<String> IMPORTANT_DIRS = Set.of("src", "docs", "resources", "main");
    private static final Set<String> LOW_SIGNAL_DIRS = Set.of("target", "build", "out", "node_modules", ".git");

    private PathScorer() {
    }

    public static double score(Path path) {
        String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String fileName = path.getFileName() != null ? path.getFileName().toString().toLowerCase(Locale.ROOT) : "";

        int depth = path.getNameCount();
        int length = normalized.length();
        String extension = extractExtension(fileName);

        double score = 0.0;
        score += Math.max(0, 26 - (depth * 1.5));
        score += Math.max(0, 35 - (length / 10.0));
        score += EXTENSION_WEIGHTS.getOrDefault(extension, 0.0);
        if (fileName.contains("readme") || fileName.contains("report") || fileName.contains("assignment")) {
            score += 8;
        }
        if (containsImportantDir(path)) {
            score += 6;
        }
        if (containsLowSignalDir(path)) {
            score -= 8;
        }
        return score;
    }

    private static boolean containsImportantDir(Path path) {
        for (Path segment : path) {
            if (IMPORTANT_DIRS.contains(segment.toString().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLowSignalDir(Path path) {
        for (Path segment : path) {
            if (LOW_SIGNAL_DIRS.contains(segment.toString().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String extractExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx == -1 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1);
    }
}
