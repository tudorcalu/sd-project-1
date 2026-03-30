package org.search.extractor;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import java.nio.file.Path;

public class DocumentExtractor {
    
    private static final Tika TIKA_INSTANCE = new Tika();

    public static ExtractedData extract(Path file) {
        try {
            Metadata metadata = new Metadata();
            String content = TIKA_INSTANCE.parseToString(file.toFile());
            
            StringBuilder metaStr = new StringBuilder();
            for (String name : metadata.names()) {
                metaStr.append(name.replace(":", "")).append("=")
                       .append(metadata.get(name)).append("; ");
            }
            
            if (content != null && content.length() > 50000) {
                content = content.substring(0, 50000);
            }
            
            return new ExtractedData(content, metaStr.toString());
        } catch (Exception e) {
            return new ExtractedData("", "error=unparseable;");
        }
    }

    public static class ExtractedData {
        private final String content;
        private final String metadata;

        public ExtractedData(String content, String metadata) {
            this.content = content != null ? content.trim() : "";
            this.metadata = metadata != null ? metadata.trim() : "";
        }

        public String getContent() { return content; }
        public String getMetadata() { return metadata; }
    }
}
