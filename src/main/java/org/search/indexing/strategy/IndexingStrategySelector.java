package org.search.indexing.strategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class IndexingStrategySelector {
    private final List<IndexingStrategy> strategies;

    public IndexingStrategySelector() {
        this(List.of(
                new ImageIndexingStrategy(),
                new TextIndexingStrategy(),
                new FallbackIndexingStrategy()
        ));
    }

    public IndexingStrategySelector(List<IndexingStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public IndexPayload extract(Path file) throws IOException {
        for (IndexingStrategy strategy : strategies) {
            if (strategy.supports(file)) {
                return strategy.extract(file);
            }
        }
        return new FallbackIndexingStrategy().extract(file);
    }
}
