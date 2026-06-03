package org.search.indexing.strategy;

import org.search.extractor.DocumentExtractor;

import java.io.IOException;
import java.nio.file.Path;

public class FallbackIndexingStrategy implements IndexingStrategy {
    @Override
    public boolean supports(Path file) {
        return true;
    }

    @Override
    public IndexPayload extract(Path file) throws IOException {
        try {
            DocumentExtractor.ExtractedData data = DocumentExtractor.extract(file);
            if (!data.getContent().isBlank()) {
                return new IndexPayload("text", data.getContent(), data.getMetadata(), null);
            }
        } catch (Exception ignored) {
        }
        return new IndexPayload("other", "", "file_type=other;", null);
    }
}
