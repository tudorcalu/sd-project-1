package org.search.application;

import org.search.db.QueryHandler;
import org.search.parser.ParsedQuery;
import org.search.parser.QueryParser;
import org.search.parser.Tokenizer;
import org.search.ranking.RankerRegistry;
import org.search.ranking.RankingStrategy;
import org.search.search.query.QueryPreprocessor;
import org.search.util.PipelineTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchService {
    private static final boolean PIPELINE_TIMING =
            !"false".equalsIgnoreCase(System.getProperty("search.pipeline.timing", "true"));

    private final QueryParser queryParser;
    private final QueryPreprocessor queryPreprocessor;

    public SearchService() {
        this(new QueryParser(), QueryPreprocessor.createDefaultPipeline());
    }

    public SearchService(QueryParser queryParser, QueryPreprocessor queryPreprocessor) {
        this.queryParser = queryParser;
        this.queryPreprocessor = queryPreprocessor;
        QueryHandler.initializeDatabase();
    }

    public List<SearchResult> search(String queryStr, RankingStrategy rankingStrategy) {
        PipelineTimer timer = PIPELINE_TIMING ? new PipelineTimer() : null;
        List<SearchResult> results = new ArrayList<>();
        ParsedQuery parsedQuery = queryParser.parse(queryStr);
        if (timer != null) {
            timer.checkpoint("parse");
        }
        if (parsedQuery.getRawQuery().isBlank()) {
            return results;
        }

        QueryHandler.recordSearchQuery(parsedQuery.getRawQuery());
        if (timer != null) {
            timer.checkpoint("recordSearchQuery");
        }

        String preprocessedFreeText = preprocessTerms(parsedQuery.getFreeTerms());
        if (timer != null) {
            timer.checkpoint("preprocessQuery");
        }

        Set<String> candidatePaths = findCandidatePaths(parsedQuery, preprocessedFreeText, timer);
        if (timer != null) {
            timer.checkpoint("ftsSearch");
        }

        List<QueryHandler.FileRecord> records = QueryHandler.getFilesByPaths(candidatePaths, 300);
        if (timer != null) {
            timer.checkpoint("fetchFilesByPaths");
        }

        for (QueryHandler.FileRecord record : records) {
            String snippet = buildSnippet(record.getContent(), queryStr);
            results.add(new SearchResult(
                    record.getPath(),
                    snippet,
                    record.getLastModified(),
                    record.getPathScore(),
                    record.getFileType(),
                    record.getDominantColor()));
        }
        if (timer != null) {
            timer.checkpoint("buildSnippetsAndResults");
        }

        Map<String, Integer> historyBoostMap = QueryHandler.getResultHitCounts(parsedQuery.getRawQuery());
        if (timer != null) {
            timer.checkpoint("fetchHistoryHitCounts");
        }

        results.sort(RankerRegistry.forStrategy(rankingStrategy).comparator(parsedQuery, historyBoostMap));
        if (timer != null) {
            timer.checkpoint("sortRanking");
        }

        List<String> topPaths = new ArrayList<>();
        for (int i = 0; i < Math.min(20, results.size()); i++) {
            topPaths.add(results.get(i).getPath());
        }
        QueryHandler.recordResultHits(parsedQuery.getRawQuery(), topPaths);
        if (timer != null) {
            timer.checkpoint("recordResultHits");
            String qLabel = parsedQuery.getRawQuery().replace('\n', ' ').trim();
            if (qLabel.length() > 120) {
                qLabel = qLabel.substring(0, 117) + "...";
            }
            System.err.print(timer.formatSummary("query=\"" + qLabel + "\""));
        }

        return results;
    }

    public String describeParsedQuery(String queryStr) {
        ParsedQuery parsed = queryParser.parse(queryStr == null ? "" : queryStr);
        return parsed.describeForUi();
    }

    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        return QueryHandler.getSuggestions(prefix.trim(), 5);
    }

    private String preprocessTerms(List<String> terms) {
        if (terms.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String term : terms) {
            for (String token : Tokenizer.tokenize(term)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(token);
            }
        }
        return queryPreprocessor.preprocess(sb.toString());
    }

    private Set<String> findCandidatePaths(ParsedQuery parsedQuery, String preprocessedFreeText, PipelineTimer timer) {
        Set<String> candidates = null;

        long tPathStart = System.nanoTime();
        if (!parsedQuery.getPathTerms().isEmpty()) {
            candidates = intersect(candidates, QueryHandler.getPathsMatchingAllPathTerms(parsedQuery.getPathTerms()));
        }
        recordPhase(timer, "findCandidates.pathTerms", tPathStart);

        long tColorStart = System.nanoTime();
        if (!parsedQuery.getColorTerms().isEmpty()) {
            candidates = intersect(candidates, QueryHandler.getPathsMatchingAllColors(parsedQuery.getColorTerms()));
        }
        recordPhase(timer, "findCandidates.colorTerms", tColorStart);

        long tContentStart = System.nanoTime();
        for (String contentTerm : parsedQuery.getContentTerms()) {
            String ftsQuery = queryPreprocessor.preprocess(String.join(" ", Tokenizer.tokenize(contentTerm)));
            Set<String> contentMatches = ftsSearch(ftsQuery, timer, "fts.content");
            candidates = intersect(candidates, contentMatches);
        }
        recordPhase(timer, "findCandidates.contentTerms", tContentStart);

        long tFreeStart = System.nanoTime();
        if (!preprocessedFreeText.isBlank()) {
            candidates = intersect(candidates, ftsSearch(preprocessedFreeText, timer, "fts.free"));
        }
        recordPhase(timer, "findCandidates.freeTerms", tFreeStart);

        if (candidates == null) {
            return QueryHandler.getAllIndexedPaths();
        }
        return candidates;
    }

    private Set<String> ftsSearch(String ftsQuery, PipelineTimer timer, String label) {
        if (ftsQuery == null || ftsQuery.isBlank()) {
            return Set.of();
        }
        if (timer == null) {
            return QueryHandler.ftsSearchPaths(ftsQuery, 5000);
        }
        return timer.time(label, () -> QueryHandler.ftsSearchPaths(ftsQuery, 5000));
    }

    private static void recordPhase(PipelineTimer timer, String label, long sectionStartNanos) {
        if (timer != null) {
            timer.recordPhase(label, System.nanoTime() - sectionStartNanos);
        }
    }

    private static Set<String> intersect(Set<String> left, Set<String> right) {
        if (left == null) {
            return new HashSet<>(right);
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection;
    }

    private static String buildSnippet(String content, String queryStr) {
        if (content == null || content.isBlank()) {
            return "";
        }
        int idx = content.toLowerCase(Locale.ROOT).indexOf(queryStr.toLowerCase(Locale.ROOT));
        if (idx != -1) {
            int start = Math.max(0, idx - 40);
            int end = Math.min(content.length(), idx + queryStr.length() + 80);
            return "..." + content.substring(start, end).replace('\n', ' ') + "...";
        }
        return content.substring(0, Math.min(content.length(), 100)).replace('\n', ' ') + "...";
    }
}
