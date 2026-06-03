package org.search.service;

import org.search.crawler.FileCrawler;
import org.search.db.QueryHandler;
import org.search.indexing.strategy.IndexPayload;
import org.search.indexing.strategy.IndexingStrategySelector;
import org.search.ranking.PathScorer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexBuilder {

    public record IndexBuildReport(
            String rootDirectory,
            int indexedFiles,
            int skippedUnchanged,
            int removedStale,
            long elapsedMillis
    ) {
        public String formatSummary() {
            double seconds = elapsedMillis / 1000.0;
            return String.format(
                    "Indexed: %d | Skipped (unchanged): %d | Removed (stale): %d | Time: %.1f s",
                    indexedFiles, skippedUnchanged, removedStale, seconds);
        }
    }

    private final ExecutorService executorService;
    private final IndexingStrategySelector strategySelector;
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final AtomicInteger skippedFiles = new AtomicInteger(0);

    public IndexBuilder(int threads) {
        this(threads, new IndexingStrategySelector());
    }

    public IndexBuilder(int threads, IndexingStrategySelector strategySelector) {
        this.executorService = Executors.newFixedThreadPool(threads);
        this.strategySelector = strategySelector;
    }

    public IndexBuildReport buildIndex(String startDirectory) {
        long startNanos = System.nanoTime();
        QueryHandler.initializeDatabase();
        System.out.println("Starting index build on: " + startDirectory);
        Map<String, QueryHandler.FileIndexState> indexedStates = QueryHandler.getIndexedFileStatesUnderRoot(startDirectory);
        Set<String> seenPaths = ConcurrentHashMap.newKeySet();

        FileCrawler.crawl(Paths.get(startDirectory), file -> {
            executorService.submit(() -> processFile(file, indexedStates, seenPaths));
        });

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        int removedFiles = QueryHandler.deleteFilesMissingFromSnapshot(startDirectory, seenPaths);
        QueryHandler.reindexFullText();
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        System.out.println("Indexing complete. Indexed files: " + processedFiles.get()
                + ", skipped unchanged files: " + skippedFiles.get()
                + ", removed stale entries: " + removedFiles
                + ", elapsed ms: " + elapsedMs);
        return new IndexBuildReport(
                startDirectory,
                processedFiles.get(),
                skippedFiles.get(),
                removedFiles,
                elapsedMs
        );
    }

    private void processFile(Path file, Map<String, QueryHandler.FileIndexState> indexedStates, Set<String> seenPaths) {
        try {
            String absolutePath = file.toAbsolutePath().toString();
            long lastModified = Files.getLastModifiedTime(file).toMillis();
            long fileSize = Files.size(file);
            seenPaths.add(absolutePath);

            QueryHandler.FileIndexState existingState = indexedStates.get(absolutePath);
            if (existingState != null
                    && existingState.getLastModified() == lastModified
                    && existingState.getFileSize() == fileSize) {
                skippedFiles.incrementAndGet();
                return;
            }

            IndexPayload payload = strategySelector.extract(file);
            QueryHandler.insertOrUpdateFile(
                    absolutePath,
                    payload.searchableText(),
                    payload.metadata(),
                    lastModified,
                    PathScorer.score(file),
                    fileSize,
                    payload.fileType(),
                    payload.dominantColor()
            );

            int count = processedFiles.incrementAndGet();
            if (count % 100 == 0) {
                System.out.println("Processed " + count + " files");
            }
        } catch (IOException e) {
            System.err.println("Error reading attributes for file: " + file);
        }
    }
}
