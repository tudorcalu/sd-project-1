package org.search.indexing.strategy;

public record IndexPayload(
        String fileType,
        String searchableText,
        String metadata,
        String dominantColor
) {
    public IndexPayload {
        fileType = fileType != null ? fileType : "other";
        searchableText = searchableText != null ? searchableText : "";
        metadata = metadata != null ? metadata : "";
    }
}
