package org.search.service;

import org.search.crawler.FileCrawler;
import org.search.db.QueryHandler;
import org.search.extractor.DocumentExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexBuilder {
    
    private final ExecutorService executorService;
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    
    public IndexBuilder(int threads) {
        this.executorService = Executors.newFixedThreadPool(threads);
    }
    
    public void buildIndex(String startDirectory) {
        QueryHandler.initializeDatabase();
        System.out.println("Starting index build on: " + startDirectory);
        
        FileCrawler.crawl(Paths.get(startDirectory), file -> {
            executorService.submit(() -> processFile(file));
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
        
        System.out.println("Indexing complete. Total files processed: " + processedFiles.get());
    }

    private void processFile(Path file) {
        try {
            long lastModified = Files.getLastModifiedTime(file).toMillis();
            DocumentExtractor.ExtractedData data = DocumentExtractor.extract(file);
            
            QueryHandler.insertOrUpdateFile(
                file.toAbsolutePath().toString(), 
                data.getContent(), 
                data.getMetadata(), 
                lastModified
            );
            
            int count = processedFiles.incrementAndGet();
            if (count % 100 == 0) {
                System.out.println("Processed " + count + " files...");
            }
        } catch (IOException e) {
            System.err.println("Error reading attributes for file: " + file);
        }
    }
}
