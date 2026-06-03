package org.search.indexing.strategy;

import org.search.extractor.DocumentExtractor;

import java.io.IOException;
import java.nio.file.Path;

public class TextIndexingStrategy implements IndexingStrategy {
    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".java")
                || name.endsWith(".xml") || name.endsWith(".json") || name.endsWith(".html")
                || name.endsWith(".csv") || name.endsWith(".log") || name.endsWith(".pdf")
                || name.endsWith(".doc") || name.endsWith(".docx");
    }

    @Override
    public IndexPayload extract(Path file) throws IOException {
        DocumentExtractor.ExtractedData data = DocumentExtractor.extract(file);
        return new IndexPayload("text", data.getContent(), data.getMetadata(), null);
    }
}
