package org.search.application;

import org.search.service.IndexBuilder;

public class IndexService {
    private final IndexBuilder indexBuilder;

    public IndexService(int threads) {
        this.indexBuilder = new IndexBuilder(threads);
    }

    public IndexBuilder.IndexBuildReport buildIndex(String rootDirectory) {
        return indexBuilder.buildIndex(rootDirectory);
    }
}
