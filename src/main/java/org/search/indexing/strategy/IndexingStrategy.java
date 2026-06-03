package org.search.indexing.strategy;

import java.io.IOException;
import java.nio.file.Path;

public interface IndexingStrategy {
    boolean supports(Path file);

    IndexPayload extract(Path file) throws IOException;
}
