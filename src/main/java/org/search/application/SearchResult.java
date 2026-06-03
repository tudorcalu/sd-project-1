package org.search.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SearchResult {
    private static final DateTimeFormatter LAST_MODIFIED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final String path;
    private final String snippet;
    private final long lastModified;
    private final double pathScore;
    private final String fileType;
    private final String dominantColor;

    public SearchResult(
            String path,
            String snippet,
            long lastModified,
            double pathScore,
            String fileType,
            String dominantColor) {
        this.path = path;
        this.snippet = snippet;
        this.lastModified = lastModified;
        this.pathScore = pathScore;
        this.fileType = fileType;
        this.dominantColor = dominantColor;
    }

    public String getPath() {
        return path;
    }

    public String getSnippet() {
        return snippet;
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

    public String getLastModifiedFormatted() {
        if (lastModified <= 0) {
            return "unknown";
        }
        return LAST_MODIFIED_FORMAT.format(Instant.ofEpochMilli(lastModified));
    }

    @Override
    public String toString() {
        String colorInfo = dominantColor != null && !dominantColor.isBlank()
                ? "\nColor: " + dominantColor
                : "";
        return "File: " + path + "\nLast modified: " + getLastModifiedFormatted()
                + "\nPathScore: " + String.format(Locale.ROOT, "%.2f", pathScore)
                + colorInfo
                + "\nSnippet: " + snippet + "\n";
    }
}
